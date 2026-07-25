package com.leets.tdd.settlement.dto.validation;

import com.leets.tdd.settlement.domain.Bank;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BankValidator implements ConstraintValidator<ValidBank, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return Bank.isSupported(value);
    }
}
