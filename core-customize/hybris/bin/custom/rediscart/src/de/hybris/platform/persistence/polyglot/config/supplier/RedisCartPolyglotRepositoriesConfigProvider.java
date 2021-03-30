package de.hybris.platform.persistence.polyglot.config.supplier;

import de.hybris.platform.persistence.polyglot.config.PolyglotRepositoriesConfigProvider;
import de.hybris.platform.persistence.polyglot.config.RepositoryConfig;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

public class RedisCartPolyglotRepositoriesConfigProvider implements PolyglotRepositoriesConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RedisCartPolyglotRepositoriesConfigProvider.class);
    private static final Set<String> RESERVED_TYPECODES = Set.of("AtomicType", "ComposedType", "CollectionType", "MapType", "AttributeDescriptor");
    private final List<RepositoryConfig> configs;
    private final ModelService modelService;
    private final TypeService typeService;
    private final PolyglotConfigSupplier properties;
    private ConfigurationService configurationService;

    public RedisCartPolyglotRepositoriesConfigProvider(ModelService modelService, TypeService typeService, ConfigurationService configurationService) {
        this(modelService, typeService, configurationService, new PropertiesPolyglotConfigSupplier());
    }

    public RedisCartPolyglotRepositoriesConfigProvider(ModelService modelService, TypeService typeService, ConfigurationService configurationService, PolyglotConfigSupplier properties) {
        this.configs = new ArrayList();
        this.modelService = modelService;
        this.typeService = typeService;
        this.properties = properties;
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void setUpPolyglotReposFromProperties() {
        this.configs.addAll(this.getRepoConfigs());
    }

    public List<RepositoryConfig> getConfigs() {
        return this.configs;
    }

    private List<RepositoryConfig> getRepoConfigs() {
        return this.properties.getRepositoryNames().stream().map(this::getRepoConfig).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private RepositoryConfig getRepoConfig(String repoName) {
        String beanName = this.properties.getBeanName(repoName);
        if(!configurationService.getConfiguration().getBoolean(repoName + ".enabled", true)){
            LOG.info("Repository {} is disabled. Ignoring it...", repoName);
            return null;
        }else {
            if (StringUtils.isEmpty(beanName)) {
                throw new IllegalArgumentException("Bad configuration parameters: beanName for repository '" + repoName + "' is not defined.");
            } else {
                Set<PropertyTypeCodeDefinition> typeCodeDefs = this.properties.getTypeCodeDefinitions(repoName);
                if (typeCodeDefs.isEmpty()) {
                    throw new IllegalArgumentException("Bad configuration parameters: type codes for repository '" + repoName + "' are not defined.");
                } else {
                    Set<String> reserved = this.filterReservedTypes(typeCodeDefs);
                    if (!reserved.isEmpty()) {
                        throw new IllegalArgumentException("Configuration of repository '" + repoName + "' contains reserved type codes " + reserved);
                    } else {
                        return new PropertyRepositoryConfig(beanName, typeCodeDefs, this.modelService, this.typeService);
                    }
                }
            }
        }
    }

    private Set<String> filterReservedTypes(Set<PropertyTypeCodeDefinition> typeCodeDefs) {
        return typeCodeDefs.stream().map(c-> c.typeCode).filter(RESERVED_TYPECODES::contains).collect(Collectors.toSet());
    }
}
