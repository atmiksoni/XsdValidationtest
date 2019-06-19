package com.xsd.validation;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XSDValidationTest {
	
	public static final String PAYER_XSD = "src/main/resources/PayerDataUpload.xsd";

	public static final String PAYER_DATA_FILE ="src/main/resources/PayerData.xml";

	
			

	@Test
	public void testPayerXmlValidation() {
		Assert.assertTrue("Payer Data xml file and XSD is get Validated",
				validateXMLSchema(PAYER_XSD,PAYER_DATA_FILE));
	}
	

	public static boolean validateXMLSchema(String xsdPath, String xmlPath) {

		try {
			SchemaFactory factory = SchemaFactory
					.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new File(xsdPath));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(new File(xmlPath)));
		} catch (IOException | SAXException e) {
			System.out.println("Exception: " + e.getMessage());
			return false;
		}
		return true;
	}

}
