package org.springframework.boot.bind;

import java.util.EnumSet;
import java.util.Locale;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;

class RelaxedConversionService
        implements ConversionService
{
    private final ConversionService conversionService;
    private final GenericConversionService additionalConverters;

    RelaxedConversionService(ConversionService conversionService)
    {
        this.conversionService = conversionService;
        this.additionalConverters = new GenericConversionService();
        DefaultConversionService.addCollectionConverters(this.additionalConverters);
        this.additionalConverters
                .addConverterFactory(new StringToEnumIgnoringCaseConverterFactory(null));
        this.additionalConverters.addConverter(new StringToCharArrayConverter());
    }

    public boolean canConvert(Class<?> sourceType, Class<?> targetType)
    {
        return ((this.conversionService != null) &&
                (this.conversionService.canConvert(sourceType, targetType))) ||
                (this.additionalConverters.canConvert(sourceType, targetType));
    }

    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType)
    {
        return ((this.conversionService != null) &&
                (this.conversionService.canConvert(sourceType, targetType))) ||
                (this.additionalConverters.canConvert(sourceType, targetType));
    }

    public <T> T convert(Object source, Class<T> targetType)
    {
        Assert.notNull(targetType, "The targetType to convert to cannot be null");
        return (T)convert(source, TypeDescriptor.forObject(source),
                TypeDescriptor.valueOf(targetType));
    }

    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType)
    {
        if (this.conversionService != null) {
            try
            {
                return this.conversionService.convert(source, sourceType, targetType);
            }
            catch (ConversionFailedException localConversionFailedException) {}
        }
        return this.additionalConverters.convert(source, sourceType, targetType);
    }

    private static class StringToEnumIgnoringCaseConverterFactory
            implements ConverterFactory<String, Enum>
    {
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType)
        {
            Class<?> enumType = targetType;
            while ((enumType != null) && (!enumType.isEnum())) {
                enumType = enumType.getSuperclass();
            }
            Assert.notNull(enumType, "The target type " + targetType.getName() + " does not refer to an enum");

            return new StringToEnum(enumType);
        }

        private class StringToEnum<T extends Enum>
                implements Converter<String, T>
        {
            private final Class<T> enumType;

            StringToEnum()
            {
                this.enumType = enumType;
            }

            public T convert(String source)
            {
                if (source.isEmpty()) {
                    return null;
                }
                source = source.trim();
                for (T candidate : EnumSet.allOf(this.enumType))
                {
                    RelaxedNames names = new RelaxedNames(candidate.name().replace('_', '-').toLowerCase(Locale.ENGLISH));
                    for (String name : names) {
                        if (name.equals(source)) {
                            return candidate;
                        }
                    }
                    if (candidate.name().equalsIgnoreCase(source)) {
                        return candidate;
                    }
                }
                throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + source);
            }
        }
    }
}

