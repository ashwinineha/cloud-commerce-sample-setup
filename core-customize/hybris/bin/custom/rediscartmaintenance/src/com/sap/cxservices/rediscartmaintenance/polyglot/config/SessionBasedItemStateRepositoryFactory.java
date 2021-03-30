package com.sap.cxservices.rediscartmaintenance.polyglot.config;

import de.hybris.platform.persistence.polyglot.ItemStateRepository;
import de.hybris.platform.persistence.polyglot.config.*;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang3.BooleanUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SessionBasedItemStateRepositoryFactory extends ItemStateRepositoryFactory {
    private static final String POLYGLOT_DISABLED_IN_SESSION = "polyglotPersistenceDisabled";

    private SessionService sessionService;

    public SessionBasedItemStateRepositoryFactory(PolyglotRepositoriesConfigProvider configProvider) {
        super(configProvider);
    }

    @Override
    public RepositoryResult getRepository(TypeInfo typeInfo) {
        boolean polyglotPersistenceDisabled = BooleanUtils.isTrue(sessionService.getAttribute(POLYGLOT_DISABLED_IN_SESSION));
        if(polyglotPersistenceDisabled){
            return RepositoryResult.empty();
        }else{
            return super.getRepository(typeInfo);
        }
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }
}
