package com.example.demo.controller;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.entity.Product;
import com.example.demo.form.ProductForm;
import com.example.demo.model.OrderDetailInfo;
import com.example.demo.model.OrderInfo;
import com.example.demo.pagination.PaginationResult;
import com.example.demo.validator.ProductFormValidator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Transactional
public class AdminController {

   @Autowired
   private OrderDAO orderDAO;

   @Autowired
   private ProductDAO productDAO;

   @Autowired
   private ProductFormValidator productFormValidator;

   @InitBinder
   public void initBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target instanceof ProductForm) {
         dataBinder.setValidator(productFormValidator);
      }
   }

   // GET: Login Page
   @GetMapping("/admin/login")
   public String login() {
      return "login";
   }

   // GET: Account Information
   @GetMapping("/admin/accountInfo")
   public String accountInfo(Model model) {
      UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      model.addAttribute("userDetails", userDetails);
      return "accountInfo";
   }

   // GET: Order List with Pagination
   @GetMapping("/admin/orderList")
   public String orderList(Model model, @RequestParam(value = "page", defaultValue = "1") int page) {
      final int MAX_RESULT = 5;
      final int MAX_NAVIGATION_PAGE = 10;

      PaginationResult<OrderInfo> paginationResult = orderDAO.listOrderInfo(page, MAX_RESULT, MAX_NAVIGATION_PAGE);
      model.addAttribute("paginationResult", paginationResult);

      return "orderList";
   }

   // GET: Product Form
   @GetMapping("/admin/product")
   public String product(Model model, @RequestParam(value = "code", required = false) String code) {
      ProductForm productForm = (code != null && !code.isEmpty())
              ? new ProductForm(productDAO.findProduct(code))
              : new ProductForm();

      if (productForm.getCode() == null) {
         productForm.setNewProduct(true);
      }

      model.addAttribute("productForm", productForm);
      return "product";
   }

   // POST: Save Product
   @PostMapping("/admin/product")
   public String productSave(
           Model model,
           @Validated @ModelAttribute("productForm") ProductForm productForm,
           BindingResult result,
           RedirectAttributes redirectAttributes) {

      if (result.hasErrors()) {
         return "product";
      }

      try {
         productDAO.save(productForm);
      } catch (Exception e) {
         String message = ExceptionUtils.getRootCauseMessage(e);
         model.addAttribute("errorMessage", message);
         return "product";
      }

      return "redirect:/productList";
   }

   // DELETE: Delete Product
   @RequestMapping(value = "/admin/product", method = RequestMethod.POST, params = "_method=delete")
   public String deleteProduct(
           @RequestParam("code") String code,
           RedirectAttributes redirectAttributes) {
      try {
         if (code != null && !code.isEmpty()) {
            productDAO.delete(code);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
         } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Product code is required.");
         }
      } catch (Exception e) {
         String message = ExceptionUtils.getRootCauseMessage(e);
         redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + message);
      }

      return "redirect:/productList";
   }

   // GET: View Order Details
   @GetMapping("/admin/order")
   public String orderView(Model model, @RequestParam("orderId") String orderId) {
      OrderInfo orderInfo = orderDAO.getOrderInfo(orderId);

      if (orderInfo == null) {
         return "redirect:/admin/orderList";
      }

      List<OrderDetailInfo> details = orderDAO.listOrderDetailInfos(orderId);
      orderInfo.setDetails(details);

      model.addAttribute("orderInfo", orderInfo);
      return "order";
   }
}
