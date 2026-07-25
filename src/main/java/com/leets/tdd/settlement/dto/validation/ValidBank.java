package com.leets.tdd.settlement.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 값이 com.leets.tdd.settlement.domain.Bank에 정의된 은행명 중 하나인지 검증한다.
 * null/빈 문자열은 통과시킨다(@NotBlank가 따로 검증하는 Bean Validation 관례를 따름).
 */
@Documented
@Constraint(validatedBy = BankValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBank {

    String message() default "지원하지 않는 은행입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
