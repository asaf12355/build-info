/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class BuildInfoRecorderLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement
    private Logger logger;

    @Requirement(role = BuildInfoRecorder.class, hint = "default", optional = false)
    BuildInfoRecorder recorder;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {

        Properties allMavenProps = new Properties();
        allMavenProps.putAll(session.getSystemProperties());
        allMavenProps.putAll(session.getUserProperties());

        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps);

        Object activateRecorderObject = allProps.get(BuildInfoRecorder.ACTIVATE_RECORDER);
        if (activateRecorderObject == null) {
            logger.debug("Disabling Artifactory Maven3 Build-Info Recorder: activation property (" +
                    BuildInfoRecorder.ACTIVATE_RECORDER + ") not found.");
            return;
        }
        if (!Boolean.valueOf(activateRecorderObject.toString())) {
            logger.debug("Disabling Artifactory Maven3 Build-Info Recorder: activation property (" +
                    BuildInfoRecorder.ACTIVATE_RECORDER + ") value is either false or invalid.");
            return;
        }
        logger.debug("Activating Artifactory Maven3 Build-Info Recorder: activation property (" +
                BuildInfoRecorder.ACTIVATE_RECORDER + ") value is true.");

        ExecutionListener existingExecutionListener = session.getRequest().getExecutionListener();
        recorder.setListenerToWrap(existingExecutionListener);
        recorder.setAllProps(allProps);
        session.getRequest().setExecutionListener(recorder);
    }
}