package com.kpmg.rcm.sourcing.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kpmg.rcm.sourcing.common.dto.RecordChange;
import com.kpmg.rcm.sourcing.common.json.dto.Dates;
import com.kpmg.rcm.sourcing.common.json.dto.Dates__1;
import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.json.dto.Source;
import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.util.RecordChangeUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecordChangeChecksumService {

    private final String twoOrMoreSpacePattern = "[\\s]{2,}";

    public String buildChecksum(Granule granule) {

        granule.getSystem().setCommonId(granule.getSystem().getCommonId().toLowerCase(Locale.ROOT));

         /*
          Below field sequence should be maintained for concatenation
          and generating checksum

                  heading
                  teaser
                  abstract
                  content
                  notes
                  notes[n].type
                  notes[n].heading
                  notes[n].content
                  dates.effective
                  dates.expire
                  dates.published
                  dates.updated
                  dates.compliance
                  linkages[n].text
                  orgs
                  orgs[n].name
                  orgs[n].aliases
                  orgs[n].type
                  topics
                  geographies
                  geographies[n].name
                  geographies[n].aliases
                  priority1content
                  priority2Content
                  priority3Content
                  originalText
         */
        StringBuilder stringBuilder = new StringBuilder();

        if (granule.getSource() == null) {
            log.info("Source is null in JSON for System: " + granule.getSystem());
            return null;
        }

        Source source = granule.getSource();
        String sourceHeading = source.getHeading() == null ? ""
                : source.getHeading().replaceAll(twoOrMoreSpacePattern,  " ");
        source.setHeading(sourceHeading);

        String sourceAbstract = source.get_abstract() == null ? ""
                : source.get_abstract().replaceAll(twoOrMoreSpacePattern, " ");
        source.set_abstract(sourceAbstract);

        String sourceContent = source.getContent() == null ? ""
                : source.getContent().replaceAll(twoOrMoreSpacePattern, " ");
        source.setContent(sourceContent);

        stringBuilder.append(source.getHeading())
                .append(source.getTeaser())
                .append(source.get_abstract())
                .append(source.getContent());

        if (!CollectionUtils.isEmpty(source.getNotes())) {
            source.getNotes().forEach(note -> {
                String noteHeading = note.getHeading() == null ? ""
                        : note.getHeading().replaceAll(twoOrMoreSpacePattern, " ");
                note.setHeading(noteHeading);

                String noteContent = note.getContent() == null ? ""
                        : note.getContent().replaceAll(twoOrMoreSpacePattern, " ");
                note.setContent(noteContent);

                stringBuilder.append(note.getType())
                        .append(note.getHeading())
                        .append(note.getContent());
            });
        }

        if (source.getDates() != null) {
            Dates dates = source.getDates();
            stringBuilder.append(dates.getEffective())
                    .append(dates.getExpire())
                    .append(dates.getPublished())
                    .append(dates.getUpdated())
                    .append(dates.getCompliance());
        }

        if (!CollectionUtils.isEmpty(source.getLinkages()))
            source.getLinkages().forEach(linkage -> stringBuilder.append(linkage.getText()));

        if (!CollectionUtils.isEmpty(source.getOrgs())) {
            source.getOrgs().forEach(org -> stringBuilder.append(org.getName())
                    .append(org.getAliases())
                    .append(org.getType()));
        }

        if (!CollectionUtils.isEmpty(source.getTopics()))
            source.getTopics().forEach(stringBuilder::append);

        stringBuilder.append(source.getPriority1content())
                .append(source.getPriority2Content())
                .append(source.getPriority3Content())
                .append(source.getOriginalText());

        //Generating the checksum
        try {
            String checksum = RecordChangeUtil.getChecksum(stringBuilder.toString());
            source.setChecksum(checksum);
            log.debug("granule Id: " + granule.getSystem().getCommonId() + ", checksum: " + checksum);
            return checksum;
        } catch (Exception e) {
            log.error("Granule Checksum generation error: ", e);
        }

        //Freeing the memory
        stringBuilder.setLength(0);
        return null;
    }

    public List<RecordChange> buildChecksumForSubGranules(List<SubGranule> subGranules, String granuleCommonId) {

        if (CollectionUtils.isEmpty(subGranules))
            return null;

        List<RecordChange> recordChanges = new ArrayList<>();

        subGranules.forEach(subGranule -> {

            if (subGranule.getId() != null)
                subGranule.setId(subGranule.getId().toLowerCase(Locale.ROOT));

            subGranule.setCommonId(granuleCommonId.toLowerCase(Locale.ROOT) + "/" + subGranule.getId());

            /*
          Below field sequence should be maintained for concatenation
          and generating checksum

                  heading
                  teaser
                  abstract
                  content
                  notes
                  notes[n].type
                  notes[n].heading
                  notes[n].content
                  dates.effective
                  dates.expire
                  dates.published
                  dates.updated
                  dates.compliance
                  linkages[n].text
                  orgs
                  orgs[n].name
                  orgs[n].aliases
                  orgs[n].type
                  topics
                  geographies
                  geographies[n].name
                  geographies[n].aliases
                  priority1content
                  priority2Content
                  priority3Content
                  originalText
         */
            StringBuilder stringBuilder = new StringBuilder();

            String sgHeading = subGranule.getHeading() == null ? ""
                    : subGranule.getHeading().replaceAll(twoOrMoreSpacePattern,  " ");
            subGranule.setHeading(sgHeading);

            String sgAbstract = subGranule.get_abstract() == null ? ""
                    : subGranule.get_abstract().replaceAll(twoOrMoreSpacePattern, " ");
            subGranule.set_abstract(sgAbstract);

            String sgContent = subGranule.getContent() == null ? ""
                    : subGranule.getContent().replaceAll(twoOrMoreSpacePattern, " ");
            subGranule.setContent(sgContent);

            stringBuilder.append(subGranule.getHeading())
                    .append(subGranule.getTeaser())
                    .append(subGranule.get_abstract())
                    .append(subGranule.getContent());

            if (!CollectionUtils.isEmpty(subGranule.getNotes())) {
                subGranule.getNotes().forEach(note -> {

                    String noteHeading = note.getHeading() == null ? ""
                            : note.getHeading().replaceAll(twoOrMoreSpacePattern, " ");
                    note.setHeading(noteHeading);

                    String noteContent = note.getContent() == null ? ""
                            : note.getContent().replaceAll(twoOrMoreSpacePattern, " ");
                    note.setContent(noteContent);

                    stringBuilder.append(note.getType())
                            .append(note.getHeading())
                            .append(note.getContent());
                });
            }

            if (subGranule.getDates() != null) {
                Dates__1 dates = subGranule.getDates();
                stringBuilder.append(dates.getEffective())
                        .append(dates.getExpire())
                        .append(dates.getPublished())
                        .append(dates.getUpdated())
                        .append(dates.getCompliance());
            }

            if (!CollectionUtils.isEmpty(subGranule.getLinkages()))
                subGranule.getLinkages().forEach(linkage -> stringBuilder.append(linkage.getText()));

            if (!CollectionUtils.isEmpty(subGranule.getOrgs())) {
                subGranule.getOrgs().forEach(org -> stringBuilder.append(org.getName())
                        .append(org.getAliases())
                        .append(org.getType()));
            }

            if (!CollectionUtils.isEmpty(subGranule.getTopics()))
                subGranule.getTopics().forEach(stringBuilder::append);

            stringBuilder.append(subGranule.getPriority1content())
                    .append(subGranule.getPriority2Content())
                    .append(subGranule.getPriority3Content())
                    .append(subGranule.getOriginalText());

            //Generating the checksum
            try {
                String checksum = RecordChangeUtil.getChecksum(stringBuilder.toString());
                subGranule.setChecksum(checksum);
                log.debug("subGranule Id: " + subGranule.getCommonId() + ", checksum: " + checksum);
                recordChanges.add(new RecordChange(subGranule.getCommonId(), checksum));
            } catch (Exception e) {
                log.error("Granule Checksum generation error: ", e);
                log.info("subGranule Id: " + subGranule.getCommonId() + ", checksum: null");
                recordChanges.add(new RecordChange(subGranule.getCommonId(), null));
            }

            //Freeing the memory
            stringBuilder.setLength(0);
        });

        return recordChanges;
    }

}
