package hu.bme.mit.theta.common.datalog;

public class StringDatalogArgument implements DatalogArgument {
	private final String content;

	public StringDatalogArgument(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof StringDatalogArgument) return content.equals(((StringDatalogArgument) o).content);
		return content.equals(o);
	}

	@Override
	public int hashCode() {
		return content.hashCode();
	}
}
