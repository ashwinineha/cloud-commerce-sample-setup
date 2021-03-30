import de.hybris.platform.core.model.order.CartEntryModel
import de.hybris.platform.core.model.order.CartModel
import de.hybris.platform.persistence.polyglot.TypeInfoFactory;
import de.hybris.platform.persistence.polyglot.PolyglotPersistence;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;

cartTypeInfo = TypeInfoFactory.getTypeInfo(43);

query = "GET {Cart}";
dbQuery = "SELECT {PK} FROM {Cart}";


sessionService.executeInLocalView(new SessionExecutionBody()
{
    @Override
    public void executeWithoutResult() {
        sessionService.setAttribute("polyglotPersistenceDisabled", true);
        cartsInDB = flexibleSearchService.search(dbQuery).getResult();

        redisCartsBeforeMigration = flexibleSearchService.search(query).getResult();
        println "redis carts before migration: " + redisCartsBeforeMigration.size();
        cartsInDB.each { cart ->
            println "cart to be migrated: " + cart.code;
            clonedCart = cloneAbstractOrderStrategy.clone(null, null, cart, cart.code, CartModel.class, CartEntryModel.class);

            modelService.save(clonedCart);

            println "migration complete for cart: " + cart.code;
        }
        redisCartsAfterMigration = flexibleSearchService.search(query).getResult();
        println "redis carts after migration: " + redisCartsAfterMigration.size();
        sessionService.setAttribute("polyglotPersistenceDisabled", false);
    }
});


