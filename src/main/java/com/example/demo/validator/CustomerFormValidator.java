package com.example.demo.validator;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.example.demo.form.CustomerForm;

@Component
public class CustomerFormValidator implements Validator {

   private final EmailValidator emailValidator = EmailValidator.getInstance();

   // This validator only supports CustomerForm validation
   @Override
   public boolean supports(Class<?> clazz) {
      return CustomerForm.class.isAssignableFrom(clazz);
   }

   @Override
   public void validate(Object target, Errors errors) {
      CustomerForm customerForm = (CustomerForm) target;

      // Validate required fields
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty.customerForm.name");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "NotEmpty.customerForm.email");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "address", "NotEmpty.customerForm.address");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "phone", "NotEmpty.customerForm.phone");

      // Validate email format
      if (!emailValidator.isValid(customerForm.getEmail())) {
         errors.rejectValue("email", "Pattern.customerForm.email");
      }
   }
}
