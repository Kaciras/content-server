package net.kaciras.blog.domain.config;

@SuppressWarnings("unchecked")
public class EnumConvertor implements Converter {

	@Override
	public boolean matchs(Class type) {
		return type.isEnum();
	}

	@Override
	public Object convert(String source, Class type) {
		try {
			return source == null ? null : Enum.valueOf(type, source);
		} catch (IllegalArgumentException e) {
			throw new ClassCastException("配置项的值不是预期的选项");
		}
	}
}
