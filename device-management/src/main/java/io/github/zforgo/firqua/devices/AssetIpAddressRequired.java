package io.github.zforgo.firqua.devices;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import io.github.zforgo.firqua.assets.Asset;
import io.github.zforgo.firqua.assets.IpAddressAware;

@Constraint(validatedBy = AssetIpAddressRequired.Validator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AssetIpAddressRequired {

    String message() default "Asset must have an IP address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AssetIpAddressRequired, Asset> {

        @Override
        public boolean isValid(Asset asset, ConstraintValidatorContext context) {
            return !(asset instanceof IpAddressAware ipAsset) || ipAsset.getIpAddress() != null;
        }
    }
}
