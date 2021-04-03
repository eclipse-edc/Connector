package com.atlas.example;

import java.util.Arrays;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossary.AtlasGlossaryExtInfo;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
/*
public class GlossaryExample {
  private final AtlasClientV2 client;

  GlossaryExample(AtlasClientV2 client) {
    this.client = client;
  }

  public AtlasGlossary createGlossary(String glossaryName) throws Exception {
    AtlasGlossary glossary = new AtlasGlossary();

    glossary.setName(glossaryName);
    glossary.setLanguage("English");
    glossary.setShortDescription("This is " + glossaryName);

    return client.createGlossary(glossary);
  }

  public void getGlossaryDetail(AtlasGlossary glossary) throws Exception {
    AtlasGlossaryExtInfo extInfo = client.getGlossaryExtInfo(glossary.getGuid());

    assert (extInfo != null);

    System.out.println("Glossary extended info: " + extInfo.getGuid() + "; name: " + extInfo.getName() + "; language: " + extInfo.getLanguage());
  }

  public AtlasGlossaryTerm createGlossaryTerm(AtlasGlossary glossary, String termName) throws Exception {
    AtlasGlossaryHeader glossaryHeader = new AtlasGlossaryHeader();
    AtlasGlossaryTerm   tempTerm     = new AtlasGlossaryTerm();

    glossaryHeader.setGlossaryGuid(glossary.getGuid());
    glossaryHeader.setDisplayText(glossary.getName());

    tempTerm.setAnchor(glossaryHeader);
    tempTerm.setName(termName);

    AtlasGlossaryTerm term = client.createGlossaryTerm(tempTerm);

    if (term != null) {
      System.out.println("Created term " + termName);
    }

    return term;
  }

  public void assignTermToEntity(AtlasGlossaryTerm term, AtlasEntity entity) throws Exception {
    AtlasRelatedObjectId objectId = new AtlasRelatedObjectId();
    objectId.setGuid(entity.getGuid());
    client.assignTermToEntities(term.getGuid(), Arrays.asList(objectId));
  }
}
*/