package com.game.repository;

import com.game.entity.Player;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Repository(value = "db")
public class PlayerRepositoryDB implements IPlayerRepository {

    private static final Logger logger = LoggerFactory.getLogger(PlayerRepositoryDB.class);

    private final SessionFactory sessionFactory;

    public PlayerRepositoryDB() {
        Properties properties = new Properties();
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:postgresql://localhost:5432/rpg");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        properties.put(Environment.USER, "postgres");
        properties.put(Environment.PASS, "postgres");
        properties.put(Environment.HBM2DDL_AUTO, "update");

        sessionFactory = new Configuration()
                .setProperties(properties)
                .addAnnotatedClass(Player.class)
                .buildSessionFactory();

        logger.info("Инициализирован PlayerRepositoryDB и создан SessionFactory");
    }

    @Override
    public List<Player> getAll(int pageNumber, int pageSize) {
        try (Session session = sessionFactory.openSession()) {
            NativeQuery<Player> query = session.createNativeQuery(
                    "SELECT * FROM rpg.player ORDER BY id OFFSET :offset LIMIT :limit", Player.class);
            query.setParameter("offset", pageNumber * pageSize, org.hibernate.type.StandardBasicTypes.INTEGER);
            query.setParameter("limit", pageSize, org.hibernate.type.StandardBasicTypes.INTEGER);

            logger.info("Получение списка игроков (page={}, size={})", pageNumber, pageSize);
            return query.getResultList();
        } catch (HibernateException e) {
            logger.error("Ошибка получения списка игроков (page={}, size={})", pageNumber, pageSize, e);
            return Collections.emptyList();
        }
    }

    @Override
    public int getAllCount() {
        try (Session session = sessionFactory.openSession()) {
            Long count = session.createNamedQuery("Player_getAllCount", Long.class)
                    .getSingleResult();
            return count.intValue();
        } catch (HibernateException e) {
            logger.error("Ошибка получения общего количества игроков", e);
            return 0;
        }
    }

    @Override
    public Player save(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(player);
            transaction.commit();
            logger.info("Игрок успешно сохранён: {}", player);
        } catch (HibernateException e) {
            logger.error("Ошибка сохранения игрока: {}", player, e);
        }
        return player;
    }

    @Override
    public Player update(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Player merged = session.merge(player);
            transaction.commit();
            logger.info("Игрок успешно обновлён: {}", merged);
            return merged;
        } catch (HibernateException e) {
            logger.error("Ошибка обновления игрока: {}", player, e);
            return player;
        }
    }

    @Override
    public Optional<Player> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<Player> query = session.createQuery("FROM Player WHERE id = :id", Player.class);
            query.setParameter("id", id);
            return Optional.ofNullable(query.getSingleResult());
        } catch (HibernateException e) {
            logger.error("Ошибка поиска игрока с id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(Player player) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(player);
            transaction.commit();
            logger.info("Игрок удалён: {}", player);
        } catch (HibernateException e) {
            logger.error("Ошибка удаления игрока: {}", player, e);
        }
    }

    @PreDestroy
    public void beforeStop() {
        logger.info("Завершение работы приложения: закрытие SessionFactory");
        sessionFactory.close();
    }
}

