package com.example.note_app.dao;

import com.example.note_app.entity.Category;
import com.example.note_app.entity.Content;
import com.example.note_app.entity.User;
import com.example.note_app.entity.relationship.Collaborator;
import com.example.note_app.entity.relationship.UserCategory;
import com.example.note_app.util.DaoService;
import com.example.note_app.util.HibernateUtility;
import com.example.note_app.util.MySQLConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.criteria.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class ContentDaoImpl implements DaoService<Content> {
    @Override
    public int addData(Content object) {
        int result;
        Session session = HibernateUtility.getSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.save(object);
            transaction.commit();
            result = 1;
        } catch (Exception e) {
            transaction.rollback();
            result = -1;
        }

        session.close();
        return result;
    }

    public int addData(Content object, User user) {
        int result;
        Session session = HibernateUtility.getSession();
        Transaction transaction = session.beginTransaction();

        object.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        object.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        object.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        object.setUpdateTime(Timestamp.valueOf(LocalDateTime.now()));

        try {
            session.save(object);
//            transaction.commit();
            session.save(new Collaborator(object.getContentId(), user.getUserId()));
            transaction.commit();
            result = 1;
        } catch (Exception e) {
            transaction.rollback();
            result = -1;
        }

        session.close();
        return result;
    }
    @Override
    public int deleteData(Content object) {
        int result;
        Session session = HibernateUtility.getSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.delete(object);
            transaction.commit();
            result = 1;
        } catch (Exception e) {
            transaction.rollback();
            result = -1;
        }

        session.close();
        return result;
    }

    public int deleteData(Content object, User user) {
        int result;
        Session session = HibernateUtility.getSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.delete(fetchCollaborator(object, user));
            session.delete(object);
            transaction.commit();
            result = 1;
        } catch (Exception e) {
            transaction.rollback();
            result = -1;
        }

        session.close();
        return result;
    }

    @Override
    public int updateData(Content object) {
        int result;
        Session session = HibernateUtility.getSession();
        Transaction transaction = session.beginTransaction();

        object.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        try {
            session.update(object);
            transaction.commit();
            result = 1;
        } catch (Exception e) {
            transaction.rollback();
            result = -1;
        }

        session.close();
        return result;
    }

    @Override
    public ObservableList<Content> fetchAll() {
        ObservableList<Content> contents = FXCollections.observableArrayList();
        Session session = HibernateUtility.getSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Content> query = builder.createQuery(Content.class);
        query.from(Content.class);

        contents.addAll(session.createQuery(query).getResultList());

        session.close();
        return contents;
    }

    public List<Content> filterData(Category categorySelected) {
        Session session = HibernateUtility.getSession();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<Content> criteriaQuery = criteriaBuilder.createQuery(Content.class);
        Root<Content> contentRoot = criteriaQuery.from(Content.class);
        Predicate predicateSelectedCategory = criteriaBuilder.equal(contentRoot.get("categories"), categorySelected);
        criteriaQuery.where(predicateSelectedCategory);
        List<Content> contents = session.createQuery(criteriaQuery).getResultList();
        session.close();
        return contents;
    }

    public ObservableList<Content> fetchByUser(User user) {
        ObservableList<Content> contents = FXCollections.observableArrayList();
        Session session = HibernateUtility.getSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Content> query = builder.createQuery(Content.class);
        Root<Content> root = query.from(Content.class);

        Predicate predicate = builder.equal(root.get(""), user.getUserId());
        query.where(predicate);

        contents.addAll(session.createQuery(query).getResultList());

        session.close();
        return contents;
    }

    public Collaborator fetchCollaborator(Content content, User user) {
        Session session = HibernateUtility.getSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Collaborator> query = builder.createQuery(Collaborator.class);
        Root<Collaborator> root = query.from(Collaborator.class);

        Predicate predicate1 = builder.equal(root.get("user_id"), user.getUserId());
        Predicate predicate2 = builder.equal(root.get("content_id"), content.getContentId());
        Predicate predicate = builder.and(predicate1, predicate2);
        query.where(predicate);

        Collaborator collaborator = session.createQuery(query).getSingleResult();

        session.close();
        return collaborator;
    }
}
