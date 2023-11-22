package org.dstadler.poi.reproduce;

import static org.apache.poi.poifs.crypt.dsig.facets.SignatureFacet.MS_DIGSIG_NS;
import static org.apache.poi.poifs.crypt.dsig.facets.SignatureFacet.XML_DIGSIG_NS;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.ooxml.util.XPathHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.dsig.KeyInfoKeySelector;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.poifs.crypt.dsig.SignaturePart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Reproduce {
	private static final String XMLSEC_VALIDATE_MANIFEST = "org.jcp.xml.dsig.validateManifests";
	private static final String XMLSEC_VALIDATE_SECURE = "org.apache.jcp.xml.dsig.secureValidation";

	private static class XPathNSContext implements NamespaceContext {
		final Map<String,String> nsMap = new HashMap<>();

		public XPathNSContext(SignatureInfo signatureInfo) {

			signatureInfo.getSignatureConfig().getNamespacePrefixes().forEach((k,v) -> nsMap.put(v,k));
			nsMap.put("dsss", MS_DIGSIG_NS);
			nsMap.put("ds", XML_DIGSIG_NS);
		}

		public String getNamespaceURI(String prefix) {
			return nsMap.get(prefix);
		}
		@SuppressWarnings("rawtypes")
		@Override
		public Iterator getPrefixes(String val) {
			return null;
		}
		public String getPrefix(String uri) {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		//System.setProperty("org.jcp.xml.dsig.secureValidation", "false");
		//System.setProperty("jdk.jar.maxSignatureFileSize", "8000000");

		try (OPCPackage pkg = OPCPackage.open(new File("office2007prettyPrintedRels.docx"), PackageAccess.READ)) {
			SignatureConfig sic = new SignatureConfig();
			SignatureInfo si = new SignatureInfo();
			si.setOpcPackage(pkg);
			si.setSignatureConfig(sic);
			boolean isValid = si.verifySignature();

			if (isValid) {
				System.out.println("Successfully validated document with " + System.getProperty("java.version"));
			} else {
				Iterator<SignaturePart> iter = si.getSignatureParts().iterator();

				KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
				XPath xpath = XPathHelper.getFactory().newXPath();
				xpath.setNamespaceContext(new XPathNSContext(si));

				Document doc;
				try (InputStream stream = si.getSignatureParts().iterator().next().getPackagePart().getInputStream()) {
					doc = DocumentHelper.readDocument(stream);
				}

				NodeList nl = (NodeList)xpath.compile("//*[@Id]").evaluate(doc, XPathConstants.NODESET);
				final int length = nl.getLength();
				for (int i=0; i<length; i++) {
					((Element)nl.item(i)).setIdAttribute("Id", true);
				}

				DOMValidateContext domValidateContext = new DOMValidateContext(keySelector, doc);
				domValidateContext.setProperty(XMLSEC_VALIDATE_MANIFEST, Boolean.TRUE);
				domValidateContext.setProperty(XMLSEC_VALIDATE_SECURE, si.getSignatureConfig().isSecureValidation());

				URIDereferencer uriDereferencer = si.getUriDereferencer();
				domValidateContext.setURIDereferencer(uriDereferencer);

				XMLSignatureFactory xmlSignatureFactory = si.getSignatureFactory();
				XMLSignature xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(domValidateContext);

				System.out.println();

				throw new IllegalStateException(
						"Not valid for " + System.getProperty("java.version") + "\n" +
						"Validate returned: " + xmlSignature.validate(domValidateContext) + "\n" +
						"HasNext: " + iter.hasNext() + ", validate: " + (iter.hasNext() ? iter.next().validate() : "N/A"));
			}
		}
	}
}
