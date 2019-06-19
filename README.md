# XsdValidationtest

Camel example for xsd validation test

package com.te.prodirectory.camel.importfxrates;

import java.io.InputStream;

import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.spring.SpringRouteBuilder;

import com.te.prodirectory.camel.converter.XmlToInHouseRateConverter;
import com.te.prodirectory.camel.inhouse.InHouseExhangeRate;
import com.te.prodirectory.camel.route.RouteConstants;

public class ImportInHouseFXRatesRoute extends SpringRouteBuilder {

    public static final String ROUTE_DIRECT_SPLIT_XML = "direct:InhouseSplitXML";
    public static final String ROUTE_PERSIST_EXCHANGE_RATE = "direct:inHousePersistExchangeRate";
    public static final String ROUTE_VALIDATE_INHOUSE_EXCHANGE_RATE = "direct:validateInhouseRate";
    public static final String FX_RATE_IN_SPLIT_XML_ROUTE = "SplitXMLRoute";
    public static final String FX_RATE_IN_HOUSE_IN_IMPORT_XML_ROUTE = "ImportInHouseXmlRoute";
    public static final String FX_RATE_IN_VALIDATE_RATE_ROUTE = "ValidateRateRoute";
    public static final String XML_RATE_INFORMATION_TAG = "<RateInformation>";
    
    private static final String DATA = "<Data>";
    
    private static final String LOGGING_FILE = "loggingFile";

    @Override
    public void configure() throws Exception {
    	
    	if (!RouteConstants.PROPERTIES.containsKey(RouteConstants.IMPORT_FX_RATES_IN_HOUSE_ROUTE_SOURCE_DIR)) {
    		return;
    	}
    	
    	getContext().getTypeConverterRegistry().addTypeConverter(InHouseExhangeRate.class, String.class, new XmlToInHouseRateConverter());
		
    	errorHandler(loggingErrorHandler(log, LoggingLevel.ERROR));
		
		from(RouteConstants.FX_RATE_IN_HOUSE_IN_START_ENDPOINT+ "?delay="+RouteConstants.PROPERTIES.getProperty(RouteConstants.IMPORT_CAMEL_IMPORT_DELAY)+"&timeUnit=MILLISECONDS&readLock=none&preMove=processing&move=../processingDone&moveFailed=../processingFailed")
				.streamCaching()
				.routeId(FX_RATE_IN_HOUSE_IN_IMPORT_XML_ROUTE)
				.log(LoggingLevel.INFO, "Inhouse FX File received : ${file:name}")
				.transacted("PROPAGATION_REQUIRES_NEW") // One transaction per file
				.doTry()
		        	.convertBodyTo(InputStream.class)
		        	.to("validator:xsd/Report.xsd")  // Validate the XML using the given XSD
		        	.to(ROUTE_DIRECT_SPLIT_XML)  // If the XML validates, send it to the correct queue
		        .doCatch(ValidationException.class)  // If the XML is not valid, put it on the error directory
		        	.to(RouteConstants.FX_RATE_IN_HOUSE_IN_START_ENDPOINT + "/xmlvalidationerror?fileName=$simple{date:now:yyyyMMddHHmmss}-${file:name}")
		        .end();
		
		// Further Split the Bank XML into Bank Offices.
		from(ROUTE_DIRECT_SPLIT_XML)
			.routeId(FX_RATE_IN_SPLIT_XML_ROUTE)
			.log(LoggingLevel.INFO, "In house FX File: "+FX_RATE_IN_SPLIT_XML_ROUTE)
			.split().tokenizeXML(DATA).streaming()
			.log(LoggingLevel.INFO, "Records Splitted into Row Nodes")
			.convertBodyTo(InHouseExhangeRate.class) // Convert XML to POJO using the XmlToExchangeRateConverter
	         .to(ROUTE_VALIDATE_INHOUSE_EXCHANGE_RATE)
		 .end();
		
        from(ROUTE_VALIDATE_INHOUSE_EXCHANGE_RATE)
        .routeId(ROUTE_VALIDATE_INHOUSE_EXCHANGE_RATE) 
        .log(LoggingLevel.INFO, "Validate Inhouse Fx Rate : "+ROUTE_VALIDATE_INHOUSE_EXCHANGE_RATE)// Convenient during testing
        .setHeader(LOGGING_FILE,method("inhouseHelper","generateLogFilename(${header.CamelFileAbsolutePath}, ${file:name})"))
        .transacted("PROPAGATION_REQUIRES_NEW")
        .doTry()
            .beanRef("inhouseHelper")
      //      .to(ROUTE_PERSIST_EXCHANGE_RATE)
        .doCatch(Throwable.class)
            .convertBodyTo(String.class)
            .to(RouteConstants.FX_RATE_IN_HOUSE_IN_START_ENDPOINT + "/persistenceerror/?fileName=$simple{date:now:yyyyMMdd}-element-${id}-${file:name}")
        .end();

//		from(ROUTE_PERSIST_EXCHANGE_RATE)
//        .routeId("inHouseXMLFxRateRoute111") 
//        .log(LoggingLevel.INFO, "In house FX File :InHouse Persist ExchangeRate")// Convenient during testing
//        .transacted("PROPAGATION_REQUIRES_NEW")  // Nested transaction, one transaction per element
//        .doTry()
//            .bean("InHouseRatePersister")
//            //.to("mock:persisted")
//        .doCatch(Throwable.class)
//            .log(LoggingLevel.ERROR, "Unable to update database")
//            .convertBodyTo(String.class)
//            .to(RouteConstants.FX_RATE_IN_HOUSE_IN_START_ENDPOINT + "/persistenceerror/?fileName=$simple{date:now:yyyyMMdd}-element-${id}-${file:name}")
//           // .to("mock:persistenceError")
//        .end();
     }
}
