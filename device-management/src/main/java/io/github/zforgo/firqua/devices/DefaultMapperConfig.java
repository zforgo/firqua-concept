package io.github.zforgo.firqua.devices;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Mapping;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "asset", source = "dto")
@Mapping(target = "organisationUnit", source = "organisationId")
@Mapping(target = "id", ignore = true)
@interface DefaultMapperConfig {
}
