package com.orientechnologies.orient.core.metadata.schema;

import com.orientechnologies.common.util.OPair;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.*;
import java.util.stream.Collectors;

public abstract class OViewImpl extends OClassImpl implements OView {

  private OViewConfig cfg;
  private Set<String>  activeIndexNames   = new HashSet<>();
  private List<String> inactiveIndexNames = new ArrayList<>();

  protected OViewImpl(OSchemaShared iOwner, String iName, OViewConfig cfg, int[] iClusterIds) {
    super(iOwner, iName, iClusterIds);
    this.cfg = cfg.copy();
  }

  protected OViewImpl(OSchemaShared iOwner, ODocument iDocument, String iName) {
    super(iOwner, iDocument, iName);
  }

  @Override
  public void fromStream() {
    super.fromStream();
    String query = document.getProperty("query");
    this.cfg = new OViewConfig(getName(), query);
    this.cfg.setUpdatable(Boolean.TRUE.equals(document.getProperty("updatable")));

    List<Map<String, String>> idxData = document.getProperty("indexes");
    for (Map<String, String> idx : idxData) {
      OViewConfig.OViewIndexConfig indexConfig = this.cfg.addIndex();
      for (Map.Entry<String, String> prop : idx.entrySet()) {
        indexConfig.addProperty(prop.getKey(), OType.valueOf(prop.getValue()));
      }
    }
    if (document.getProperty("updateIntervalSeconds") instanceof Integer) {
      cfg.setUpdateIntervalSeconds(document.getProperty("updateIntervalSeconds"));
    }
    if (document.getProperty("updateStrategy") instanceof String) {
      cfg.setUpdateStrategy(document.getProperty("updateStrategy"));
    }
    if (document.getProperty("watchClasses") instanceof List) {
      cfg.setWatchClasses(document.getProperty("watchClasses"));
    }
    if (document.getProperty("originRidField") instanceof String) {
      cfg.setOriginRidField(document.getProperty("originRidField"));
    }
    if (document.getProperty("nodes") instanceof List) {
      cfg.setNodes(document.getProperty("nodes"));
    }
    if (document.getProperty("activeIndexNames") instanceof Set) {
      activeIndexNames = document.getProperty("activeIndexNames");
    }
    if (document.getProperty("inactiveIndexNames") instanceof List) {
      inactiveIndexNames = document.getProperty("inactiveIndexNames");
    }

  }

  @Override
  public ODocument toStream() {
    ODocument result = super.toStream();
    result.setProperty("query", cfg.getQuery());
    result.setProperty("updatable", cfg.isUpdatable());

    List<Map<String, String>> indexes = new ArrayList<>();
    for (OViewConfig.OViewIndexConfig idx : cfg.indexes) {
      Map<String, String> indexDescriptor = new HashMap<>();
      for (OPair<String, OType> s : idx.props) {
        indexDescriptor.put(s.key, s.value.toString());
      }
      indexes.add(indexDescriptor);
    }
    result.setProperty("indexes", indexes);
    result.setProperty("updateIntervalSeconds", cfg.getUpdateIntervalSeconds());
    result.setProperty("updateStrategy", cfg.getUpdateStrategy());
    result.setProperty("watchClasses", cfg.getWatchClasses());
    result.setProperty("originRidField", cfg.getOriginRidField());
    result.setProperty("nodes", cfg.getNodes());
    result.setProperty("activeIndexNames", activeIndexNames);
    result.setProperty("inactiveIndexNames", inactiveIndexNames);
    return result;
  }

  @Override
  public ODocument toNetworkStream() {
    ODocument result = super.toNetworkStream();
    result.setProperty("query", cfg.getQuery());
    result.setProperty("updatable", cfg.isUpdatable());
    List<Map<String, String>> indexes = new ArrayList<>();
    for (OViewConfig.OViewIndexConfig idx : cfg.indexes) {
      Map<String, String> indexDescriptor = new HashMap<>();
      for (OPair<String, OType> s : idx.props) {
        indexDescriptor.put(s.key, s.value.toString());
      }
      indexes.add(indexDescriptor);
    }
    result.setProperty("indexes", indexes);
    result.setProperty("updateIntervalSeconds", cfg.getUpdateIntervalSeconds());
    result.setProperty("updateStrategy", cfg.getUpdateStrategy());
    result.setProperty("watchClasses", cfg.getWatchClasses());
    result.setProperty("originRidField", cfg.getOriginRidField());
    result.setProperty("nodes", cfg.getNodes());
    result.setProperty("activeIndexNames", activeIndexNames);
    result.setProperty("inactiveIndexNames", inactiveIndexNames);
    return result;
  }

  @Override
  public String getQuery() {
    return cfg.getQuery();
  }

  public long count(final boolean isPolymorphic) {
    acquireSchemaReadLock();
    try {
      return getDatabase().countView(getName());
    } finally {
      releaseSchemaReadLock();
    }
  }

  @Override
  public int getUpdateIntervalSeconds() {
    return cfg.updateIntervalSeconds;
  }

  public List<String> getWatchClasses() {
    return cfg.getWatchClasses();
  }

  @Override
  public String getOriginRidField() {
    return cfg.getOriginRidField();
  }

  @Override
  public boolean isUpdatable() {
    return cfg.isUpdatable();
  }

  @Override
  public List<String> getNodes() {
    return cfg.getNodes();
  }

  @Override
  public List<OViewConfig.OViewIndexConfig> getRequiredIndexesInfo() {
    return cfg.getIndexes();
  }

  public Set<OIndex<?>> getClassIndexes() {
    if (activeIndexNames == null || activeIndexNames.isEmpty()) {
      return new HashSet<>();
    }
    acquireSchemaReadLock();
    try {

      final OIndexManager idxManager = getDatabase().getMetadata().getIndexManager();
      if (idxManager == null)
        return new HashSet<>();

      return activeIndexNames.stream().map(name -> idxManager.getIndex(name)).filter(Objects::nonNull).collect(Collectors.toSet());
    } finally {
      releaseSchemaReadLock();
    }
  }

  @Override
  public void getClassIndexes(final Collection<OIndex<?>> indexes) {
    acquireSchemaReadLock();
    try {
      final OIndexManager idxManager = getDatabase().getMetadata().getIndexManager();
      if (idxManager == null)
        return;

      activeIndexNames.stream().map(name -> idxManager.getIndex(name)).filter(Objects::nonNull).forEach(x -> indexes.add(x));
      idxManager.getClassIndexes(name, indexes);
    } finally {
      releaseSchemaReadLock();
    }
  }

  public void inactivateIndexes() {
    acquireSchemaReadLock();
    try {
      this.inactiveIndexNames.addAll(activeIndexNames);
      this.activeIndexNames.clear();
    } finally {
      releaseSchemaReadLock();
    }
  }

  public void inactivateIndex(String name) {
    acquireSchemaReadLock();
    try {
      this.activeIndexNames.remove(name);
      this.inactiveIndexNames.add(name);
    } finally {
      releaseSchemaReadLock();
    }
  }

  public List<String> getInactiveIndexes() {
    return inactiveIndexNames;
  }

  public void addActiveIndexes(List<String> names) {
    acquireSchemaReadLock();
    try {
      this.activeIndexNames.addAll(names);
    } finally {
      releaseSchemaReadLock();
    }
  }
}
