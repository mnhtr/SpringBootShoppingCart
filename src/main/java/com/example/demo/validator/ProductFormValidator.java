package com.example.demo.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.ProductForm;

@Component
public class ProductFormValidator implements Validator {

   @Autowired
   private ProductDAO productDAO;

   @Override
   public boolean supports(Class<?> clazz) {
      return ProductForm.class.isAssignableFrom(clazz);
   }

   @Override
   public void validate(Object target, Errors errors) {
      if (!(target instanceof ProductForm)) {
         throw new IllegalArgumentException("Target must be an instance of ProductForm");
      }

      ProductForm productForm = (ProductForm) target;

      // Validate required fields
      validateRequiredFields(errors);

      // Validate the product code
      validateProductCode(productForm, errors);
   }

   private void validateRequiredFields(Errors errors) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "code", "NotEmpty.productForm.code");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty.productForm.name");
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "price", "NotEmpty.productForm.price");
   }

   private void validateProductCode(ProductForm productForm, Errors errors) {
      String code = productForm.getCode();

      if (code != null && !code.trim().isEmpty()) {
         // Reject if the code contains only whitespace
         if (code.trim().length() != code.length()) {
            errors.rejectValue("code", "Pattern.productForm.code");
         }
         // Check for duplicate product code if it's a new product
         else if (productForm.isNewProduct()) {
            Product existingProduct = productDAO.findProduct(code);
            if (existingProduct != null) {
               errors.rejectValue("code", "Duplicate.productForm.code");
            }
         }
      }
   }
}
