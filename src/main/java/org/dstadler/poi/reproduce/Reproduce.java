package org.dstadler.poi.reproduce;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.crypt.dsig.KeyInfoKeySelector;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.poifs.crypt.dsig.SignaturePart;
import org.w3c.dom.Document;

public class Reproduce {
	public static void main(String[] args) throws Exception {
		// these were listed in release-notes for JDK 21, but none has an impact
		//System.setProperty("org.jcp.xml.dsig.secureValidation", "false");
		//System.setProperty("jdk.jar.maxSignatureFileSize", "8000000");

		final File[] files = Objects.requireNonNull(
				new File(".").listFiles((FilenameFilter)
					new OrFileFilter(
					new SuffixFileFilter(".xlsx"),
					new SuffixFileFilter(".pptx"),
					new SuffixFileFilter(".docx")
					)),
				"Directory " + new File(".").getAbsolutePath() + " not found");
		for (File file : files) {
			System.out.println("Testing file " + file + " with " + System.getProperty("java.version"));
			try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
				SignatureConfig sic = new SignatureConfig();
				SignatureInfo si = new SignatureInfo();
				si.setOpcPackage(pkg);
				si.setSignatureConfig(sic);
				boolean isValid = si.verifySignature();

				if (isValid) {
					System.out.println("Successfully validated document with " + System.getProperty("java.version"));
				} else if (!si.getSignatureParts().iterator().hasNext()) {
					System.out.println("=> Not found with " + System.getProperty("java.version"));
				} else {
					Iterator<SignaturePart> iter = si.getSignatureParts().iterator();

					Document doc;
					try (InputStream stream = si.getSignatureParts().iterator().next().getPackagePart().getInputStream()) {
						doc = DocumentHelper.readDocument(stream);
					}

					KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
					DOMValidateContext domValidateContext = new DOMValidateContext(keySelector, doc);
					XMLSignatureFactory xmlSignatureFactory = si.getSignatureFactory();
					XMLSignature xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(domValidateContext);

					System.out.println();

					System.out.println(
							"Not valid for " + System.getProperty("java.version") + "\n" +
									"    Validate returned: " + xmlSignature.validate(domValidateContext) + "\n" +
									"    HasNext: " + iter.hasNext() + ", validate: " + (iter.hasNext() ? iter.next().validate() :
									"N/A"));
				}
			}
		}
	}
}
