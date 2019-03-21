package com.edu.hibernateintergration.service.impl;

import com.edu.hibernateintergration.dao.AbstractDao;
import com.edu.hibernateintergration.model.PersistentLogins;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
@Transactional
public class PersistentTokenDaoImp extends AbstractDao<String,PersistentLogins> implements PersistentTokenRepository {

    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogins logins=new PersistentLogins();
        logins.setUsername(token.getUsername());
        logins.setSeries(token.getSeries());
        logins.setToken(token.getTokenValue());
        logins.setLastUsed(token.getDate());
        persist(logins);
    }

    public void updateToken(String series, String tokenValue, Date lastUsed) {
        PersistentLogins persistentLogin = getByKey(series);
        persistentLogin.setToken(tokenValue);
        persistentLogin.setLastUsed(lastUsed);
        update(persistentLogin);
    }

    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        try {
            Criteria crit = createEntityCriteria();
            crit.add(Restrictions.eq("series", seriesId));
            PersistentLogins persistentLogin = (PersistentLogins) crit.uniqueResult();

            return new PersistentRememberMeToken(persistentLogin.getUsername(), persistentLogin.getSeries(),
                    persistentLogin.getToken(), persistentLogin.getLastUsed());
        } catch (Exception e) {
            return null;
        }
    }

    public void removeUserTokens(String username) {
        Criteria crit = createEntityCriteria();
        crit.add(Restrictions.eq("username", username));
        PersistentLogins persistentLogin = (PersistentLogins) crit.uniqueResult();
        if (persistentLogin != null) {
            delete(persistentLogin);
        }
    }
}
