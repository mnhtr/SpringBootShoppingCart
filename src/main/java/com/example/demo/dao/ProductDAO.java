package com.example.demo.dao;

import com.example.demo.entity.Product;
import com.example.demo.form.ProductForm;
import com.example.demo.model.ProductInfo;
import com.example.demo.pagination.PaginationResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.Date;

@Repository
@Transactional
public class ProductDAO {

    @Autowired
    private SessionFactory sessionFactory;

    public Product findProduct(String code) {
        try {
            String sql = "Select e from " + Product.class.getName() + " e Where e.code = :code";
            Session session = this.sessionFactory.getCurrentSession();
            Query<Product> query = session.createQuery(sql, Product.class);
            query.setParameter("code", code);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public ProductInfo findProductInfo(String code) {
        Product product = this.findProduct(code);
        return product == null ? null : new ProductInfo(product.getCode(), product.getName(), product.getPrice());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(ProductForm productForm) {
        Session session = this.sessionFactory.getCurrentSession();
        String code = productForm.getCode();

        Product product = code != null ? this.findProduct(code) : null;
        boolean isNew = product == null;

        if (isNew) {
            product = new Product();
            product.setCreateDate(new Date());
        }

        product.setCode(code);
        product.setName(productForm.getName());
        product.setPrice(productForm.getPrice());

        if (productForm.getFileData() != null) {
            try {
                byte[] image = productForm.getFileData().getBytes();
                if (image.length > 0) {
                    product.setImage(image);
                }
            } catch (IOException e) {
                // Log the exception if needed
            }
        }

        if (isNew) {
            session.persist(product);
        }

        session.flush(); // Ensure data is saved immediately
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void delete(String code) {
        Product product = findProduct(code);
        if (product != null) {
            Session session = this.sessionFactory.getCurrentSession();
            product.setStatus(false); // Update status to false
            session.update(product); // Persist the change
            session.flush(); // Ensure the update is executed immediately
        }
    }

    public PaginationResult<ProductInfo> queryProducts(int page, int maxResult, int maxNavigationPage, String likeName) {
        StringBuilder sql = new StringBuilder("Select new ");
        sql.append(ProductInfo.class.getName())
                .append("(p.code, p.name, p.price) from ")
                .append(Product.class.getName()).append(" p");

        // Always filter by status = 1
        sql.append(" Where p.status = 1");

        // Add additional filter for likeName if provided
        if (likeName != null && !likeName.isEmpty()) {
            sql.append(" and lower(p.name) like :likeName");
        }

        sql.append(" order by p.createDate desc");

        Session session = this.sessionFactory.getCurrentSession();
        Query<ProductInfo> query = session.createQuery(sql.toString(), ProductInfo.class);

        // Set parameter for likeName if provided
        if (likeName != null && !likeName.isEmpty()) {
            query.setParameter("likeName", "%" + likeName.toLowerCase() + "%");
        }

        return new PaginationResult<>(query, page, maxResult, maxNavigationPage);
    }

}
