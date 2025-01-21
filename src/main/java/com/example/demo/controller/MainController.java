package com.example.demo.controller;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.CustomerForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;
import com.example.demo.utils.Utils;
import com.example.demo.validator.CustomerFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Transactional
public class MainController {

   @Autowired
   private OrderDAO orderDAO;

   @Autowired
   private ProductDAO productDAO;

   @Autowired
   private CustomerFormValidator customerFormValidator;

   @InitBinder
   public void initBinder(WebDataBinder dataBinder) {
      if (dataBinder.getTarget() instanceof CustomerForm) {
         dataBinder.setValidator(customerFormValidator);
      }
   }

   @GetMapping("/403")
   public String accessDenied() {
      return "/403";
   }

   @GetMapping("/")
   public String home() {
      return "index";
   }

   @GetMapping("/productList")
   public String listProduct(
           Model model,
           @RequestParam(value = "name", defaultValue = "") String likeName,
           @RequestParam(value = "page", defaultValue = "1") int page) {

      final int MAX_RESULT = 8;
      final int MAX_NAVIGATION_PAGE = 10;

      PaginationResult<ProductInfo> result = productDAO.queryProducts(page, MAX_RESULT, MAX_NAVIGATION_PAGE, likeName);
      model.addAttribute("paginationProducts", result);

      return "productList";
   }

   @GetMapping("/buyProduct")
   public String buyProduct(
           HttpServletRequest request,
           @RequestParam(value = "code", defaultValue = "") String code) {

      if (!code.isEmpty()) {
         Product product = productDAO.findProduct(code);
         if (product != null) {
            CartInfo cartInfo = Utils.getCartInSession(request);
            cartInfo.addProduct(new ProductInfo(product), 1);
         }
      }
      return "redirect:/shoppingCart";
   }

   @GetMapping("/shoppingCartRemoveProduct")
   public String removeProduct(
           HttpServletRequest request,
           @RequestParam(value = "code", defaultValue = "") String code) {

      if (!code.isEmpty()) {
         Product product = productDAO.findProduct(code);
         if (product != null) {
            CartInfo cartInfo = Utils.getCartInSession(request);
            cartInfo.removeProduct(new ProductInfo(product));
         }
      }
      return "redirect:/shoppingCart";
   }

   @PostMapping("/shoppingCart")
   public String updateCartQuantity(
           HttpServletRequest request,
           @ModelAttribute("cartForm") CartInfo cartForm) {

      Utils.getCartInSession(request).updateQuantity(cartForm);
      return "redirect:/shoppingCart";
   }

   @GetMapping("/shoppingCart")
   public String viewCart(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);
      model.addAttribute("cartForm", cartInfo);
      model.addAttribute("myCart", cartInfo);
      return "shoppingCart";
   }

   @GetMapping("/shoppingCartCustomer")
   public String customerForm(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      }

      model.addAttribute("customerForm", new CustomerForm(cartInfo.getCustomerInfo()));
      return "shoppingCartCustomer";
   }

   @PostMapping("/shoppingCartCustomer")
   public String saveCustomerInfo(
           HttpServletRequest request,
           @Validated @ModelAttribute("customerForm") CustomerForm customerForm,
           BindingResult result) {

      if (result.hasErrors()) {
         customerForm.setValid(false);
         return "shoppingCartCustomer";
      } else {
         customerForm.setValid(true);
      }

      CartInfo cartInfo = Utils.getCartInSession(request);
      cartInfo.setCustomerInfo(new CustomerInfo(customerForm));
      return "redirect:/shoppingCartConfirmation";
   }

   @GetMapping("/shoppingCartConfirmation")
   public String confirmCart(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      }

      if (!cartInfo.isValidCustomer()) {
         return "redirect:/shoppingCartCustomer";
      }

      model.addAttribute("myCart", cartInfo);
      return "shoppingCartConfirmation";
   }

   @PostMapping("/shoppingCartConfirmation")
   public String saveOrder(HttpServletRequest request) {
      CartInfo cartInfo = Utils.getCartInSession(request);

      if (cartInfo.isEmpty() || !cartInfo.isValidCustomer()) {
         return "redirect:/shoppingCart";
      }

      try {
         orderDAO.saveOrder(cartInfo);
      } catch (Exception e) {
         return "shoppingCartConfirmation";
      }

      Utils.removeCartInSession(request);
      Utils.storeLastOrderedCartInSession(request, cartInfo);
      return "redirect:/shoppingCartFinalize";
   }

   @GetMapping("/shoppingCartFinalize")
   public String finalizeCart(HttpServletRequest request, Model model) {
      CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);

      if (lastOrderedCart == null) {
         return "redirect:/shoppingCart";
      }

      model.addAttribute("lastOrderedCart", lastOrderedCart);
      return "shoppingCartFinalize";
   }

   @GetMapping("/productImage")
   public void getProductImage(
           HttpServletResponse response,
           @RequestParam("code") String code) throws IOException {

      Product product = productDAO.findProduct(code);

      if (product != null && product.getImage() != null) {
         response.setContentType("image/jpeg");
         response.getOutputStream().write(product.getImage());
      }
      response.getOutputStream().close();
   }
}
