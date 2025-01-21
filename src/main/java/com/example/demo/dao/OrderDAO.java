package com.example.demo.dao;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.Product;
import com.example.demo.model.*;
import com.example.demo.pagination.PaginationResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class OrderDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ProductDAO productDAO;

    /**
     * Retrieves the maximum order number.
     *
     * @return Maximum order number or 0 if none exists.
     */
    private int getMaxOrderNum() {
        String sql = "Select max(o.orderNum) from " + Order.class.getName() + " o";
        Session session = sessionFactory.getCurrentSession();
        Query<Integer> query = session.createQuery(sql, Integer.class);
        return query.uniqueResultOptional().orElse(0);
    }

    /**
     * Saves an order to the database based on cart information.
     *
     * @param cartInfo The cart information.
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(CartInfo cartInfo) {
        Session session = sessionFactory.getCurrentSession();

        // Create a new order
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNum(getMaxOrderNum() + 1);
        order.setOrderDate(new Date());
        order.setAmount(cartInfo.getAmountTotal());

        // Set customer details
        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());

        session.persist(order);

        // Save order details
        for (CartLineInfo line : cartInfo.getCartLines()) {
            OrderDetail detail = new OrderDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setOrder(order);
            detail.setAmount(line.getAmount());
            detail.setPrice(line.getProductInfo().getPrice());
            detail.setQuanity(line.getQuantity());

            Product product = productDAO.findProduct(line.getProductInfo().getCode());
            detail.setProduct(product);

            session.persist(detail);
        }

        cartInfo.setOrderNum(order.getOrderNum());
        session.flush();
    }

    /**
     * Retrieves a paginated list of order information.
     *
     * @param page              Current page number.
     * @param maxResult         Maximum results per page.
     * @param maxNavigationPage Maximum navigation pages.
     * @return Paginated result of order information.
     */
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage) {
        String sql = "Select new " + OrderInfo.class.getName() +
                "(ord.id, ord.orderDate, ord.orderNum, ord.amount, " +
                "ord.customerName, ord.customerAddress, ord.customerEmail, ord.customerPhone) " +
                "from " + Order.class.getName() + " ord order by ord.orderNum desc";

        Session session = sessionFactory.getCurrentSession();
        Query<OrderInfo> query = session.createQuery(sql, OrderInfo.class);
        return new PaginationResult<>(query, page, maxResult, maxNavigationPage);
    }

    /**
     * Finds an order by its ID.
     *
     * @param orderId The order ID.
     * @return The order or null if not found.
     */
    public Order findOrder(String orderId) {
        Session session = sessionFactory.getCurrentSession();
        return session.find(Order.class, orderId);
    }

    /**
     * Retrieves order information by its ID.
     *
     * @param orderId The order ID.
     * @return Order information or null if not found.
     */
    public OrderInfo getOrderInfo(String orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            return null;
        }
        return new OrderInfo(order.getId(), order.getOrderDate(), order.getOrderNum(),
                order.getAmount(), order.getCustomerName(), order.getCustomerAddress(),
                order.getCustomerEmail(), order.getCustomerPhone());
    }

    /**
     * Retrieves order detail information for a given order ID.
     *
     * @param orderId The order ID.
     * @return List of order detail information.
     */
    public List<OrderDetailInfo> listOrderDetailInfos(String orderId) {
        String sql = "Select new " + OrderDetailInfo.class.getName() +
                "(d.id, d.product.code, d.product.name, d.quanity, d.price, d.amount) " +
                "from " + OrderDetail.class.getName() + " d where d.order.id = :orderId";

        Session session = sessionFactory.getCurrentSession();
        Query<OrderDetailInfo> query = session.createQuery(sql, OrderDetailInfo.class);
        query.setParameter("orderId", orderId);

        return query.getResultList();
    }
}
