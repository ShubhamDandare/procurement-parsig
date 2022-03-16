package com.kpmg.rcm.sourcing.russia.constants;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class RussiaSourceConstants {

	public enum Source {
		PARALEGAL, CENTRALBANK;
	}

	public static final String emptyString = "";
	public static final String backSlashQuote = "\"";
	public static final String newLineCharacter = "\n";
	public static final String paralegalCommonKeyPrefix = "russia/paralegal/";
	public static final String orgEditionString = "Original Edition";

	public static final String paraLegalDomain = "http://pravo.gov.ru/proxy/ips/";
	public static final String paraLegalFederalLawBaseUrl = "?list_itself=&bpas=cd00000&page=first";
	public static final String paraLegalFederalLawVerUrl = "?docbody=&fulltext=1";// &nd=
	public static final String paraLegalFederalLawDocUrl = "?doc_itself=&fulltext=1";// &nd=&rdk=

	public static final List<String> regexParaLegalFederalLaw = new LinkedList<>(
			Arrays.asList("Статья \\d+\\.(.*)", "\\d+\\."));

	@AllArgsConstructor
	public enum subSource {
		FederalLaw("102000505"), Order("102000497"), Code("102000486");

		@Getter
		private final String value;
	};

}
