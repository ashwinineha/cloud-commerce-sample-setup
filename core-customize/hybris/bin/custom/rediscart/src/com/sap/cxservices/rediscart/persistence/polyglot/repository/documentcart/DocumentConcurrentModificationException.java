/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart;

import de.hybris.platform.persistence.polyglot.uow.PolyglotPersistenceConcurrentModificationException;


public class DocumentConcurrentModificationException extends PolyglotPersistenceConcurrentModificationException {
    public DocumentConcurrentModificationException(final String msg) {
        super(msg);
    }

    public static DocumentConcurrentModificationException missingDocumentForRemoval(final Entity itemToRemove) {
        return new DocumentConcurrentModificationException(String.format("Couldn't remove entity ('%s') because the Document it belongs to has been removed in the meantime.", itemToRemove.getId()));
    }

    public static DocumentConcurrentModificationException missingDocumentForModification(final EntityModification modification) {
        return new DocumentConcurrentModificationException(String.format("Couldn't modify entity ('%s') because the Document it belongs to has been removed in the meantime.", modification.getId()));
    }

    public static DocumentConcurrentModificationException missingDocumentForCreation(final EntityCreation creation) {
        return new DocumentConcurrentModificationException(String.format("Couldn't create entity ('%s') because the Document it belongs to has been removed in the meantime.", creation.getId()));
    }

    public static DocumentConcurrentModificationException documentAlreadyExist(final Document document) {
        return new DocumentConcurrentModificationException(String.format("Couldn't create document with id '%s'. It already exists.", document.getRootId()));
    }

    public static DocumentConcurrentModificationException documentHasBeenRemoved(final Document document) {
        return new DocumentConcurrentModificationException(String.format("Document with id '%s' has been removed.", document.getRootId()));
    }

    public static DocumentConcurrentModificationException documentHasBeenModified(final Document document) {
        return new DocumentConcurrentModificationException(String.format("Document with id '%s'  and version '%s' has been modified.", document.getRootId(), document.getVersion()));
    }

}
