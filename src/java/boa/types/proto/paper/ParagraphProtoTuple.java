package boa.types.proto.paper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import boa.types.BoaProtoList;
import boa.types.BoaProtoTuple;
import boa.types.BoaString;
import boa.types.BoaType;
import boa.types.proto.enums.ParagraphKindProtoMap;

public class ParagraphProtoTuple extends BoaProtoTuple {
	private final static List<BoaType> members = new ArrayList<BoaType>();
	private final static Map<String, Integer> names = new HashMap<String, Integer>();

	static {
		int counter = 0;

		names.put("text", counter++);
		members.add(new BoaString());

		names.put("cite_spans", counter++);
		members.add(new BoaProtoList(new CitationProtoTuple()));

		names.put("ref_spans", counter++);
		members.add(new BoaProtoList(new CitationProtoTuple()));

		names.put("kind", counter++);
		members.add(new ParagraphKindProtoMap());
	}

	public ParagraphProtoTuple() {
		super(members, names);
	}

	@Override
	public String toJavaType() {
		return "boa.types.Toplevel.Paragraph";
	}

}
