package top.infra.maven.extension.main;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static top.infra.maven.extension.main.MavenBuildExtensionOption.ORIGIN_REPO;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_FALSE;
import static top.infra.maven.shared.extension.Constants.BOOL_STRING_TRUE;
import static top.infra.maven.shared.extension.Constants.PHASE_CLEAN;
import static top.infra.maven.shared.extension.Constants.PHASE_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PHASE_INSTALL;
import static top.infra.maven.shared.extension.Constants.PHASE_PACKAGE;
import static top.infra.maven.shared.extension.Constants.PHASE_SITE;
import static top.infra.maven.shared.extension.Constants.PHASE_SITE_DEPLOY;
import static top.infra.maven.shared.extension.Constants.PHASE_VERIFY;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_CLEAN_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_JAVADOC_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_MAVEN_SOURCE_SKIP;
import static top.infra.maven.shared.extension.Constants.PROP_NEXUS2_STAGING;
import static top.infra.maven.shared.extension.Constants.PROP_PUBLISH_TO_REPO;
import static top.infra.maven.shared.extension.MavenOption.GENERATEREPORTS;
import static top.infra.maven.shared.extension.VcsProperties.GIT_REF_NAME;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOptionContext;
import top.infra.maven.shared.DefaultCiOptionContext;
import top.infra.maven.test.logging.LoggerSlf4jImpl;

public class MavenGoalEditorTest {

    private static final Logger logger = LoggerFactory.getLogger(MavenGoalEditorTest.class);

    @Test
    public void testSimpleGoals() {
        final Entry<List<String>, Properties> cleanInstall = goalsAndUserProps(blankCiOptCtx(), asList(PHASE_CLEAN, PHASE_INSTALL));
        assertTrue(cleanInstall.getKey().contains(PHASE_CLEAN));
        assertTrue(cleanInstall.getKey().contains(PHASE_INSTALL));
        assertEquals(2, cleanInstall.getKey().size());

        final Entry<List<String>, Properties> cleanPackage = goalsAndUserProps(blankCiOptCtx(), asList(PHASE_CLEAN, PHASE_PACKAGE));
        assertTrue(cleanPackage.getKey().contains(PHASE_CLEAN));
        assertTrue(cleanPackage.getKey().contains(PHASE_PACKAGE));
        assertEquals(2, cleanPackage.getKey().size());

        final Entry<List<String>, Properties> cleanVerify = goalsAndUserProps(blankCiOptCtx(), asList(PHASE_CLEAN, PHASE_VERIFY));
        assertTrue(cleanVerify.getKey().contains(PHASE_CLEAN));
        assertTrue(cleanVerify.getKey().contains(PHASE_VERIFY));
        assertEquals(2, cleanVerify.getKey().size());
    }

    @Test
    public void testSkipDeployGoalsIfNotPublishToRepo() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();
        ciOptionContext.getUserProperties().setProperty(PROP_PUBLISH_TO_REPO, BOOL_STRING_FALSE);

        final List<String> requestedGoals = asList(PHASE_CLEAN, PHASE_DEPLOY, PHASE_SITE, PHASE_SITE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_CLEAN));
        assertTrue(resultGoals.contains(PHASE_SITE));
        assertTrue(resultGoals.contains(PHASE_SITE_DEPLOY));
        assertEquals(3, resultGoals.size());
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    @Test
    public void testSonarGoals() {
        final String sonarGoal = "sonar:sonar";
        final CiOptionContext ctxRefMaster = blankCiOptCtx();
        ctxRefMaster.getUserProperties().setProperty(GIT_REF_NAME.getPropertyName(), "master");
        ctxRefMaster.getUserProperties().setProperty(ORIGIN_REPO.getPropertyName(), BOOL_STRING_TRUE);
        assertFalse(goalsAndUserProps(ctxRefMaster, singletonList(sonarGoal)).getKey().contains(sonarGoal));
        ctxRefMaster.getUserProperties().setProperty(ORIGIN_REPO.getPropertyName(), BOOL_STRING_FALSE);
        assertFalse(goalsAndUserProps(ctxRefMaster, singletonList(sonarGoal)).getKey().contains(sonarGoal));

        final CiOptionContext ctxRefDevelop = blankCiOptCtx();
        ctxRefDevelop.getUserProperties().setProperty(GIT_REF_NAME.getPropertyName(), "develop");
        ctxRefDevelop.getUserProperties().setProperty(ORIGIN_REPO.getPropertyName(), BOOL_STRING_TRUE);
        assertTrue(goalsAndUserProps(ctxRefDevelop, singletonList(sonarGoal)).getKey().contains(sonarGoal));
        ctxRefDevelop.getUserProperties().setProperty(ORIGIN_REPO.getPropertyName(), BOOL_STRING_FALSE);
        assertFalse(goalsAndUserProps(ctxRefDevelop, singletonList(sonarGoal)).getKey().contains(sonarGoal));

        final CiOptionContext ctxRefAbsent = blankCiOptCtx();
        ctxRefAbsent.getUserProperties().setProperty(ORIGIN_REPO.getPropertyName(), BOOL_STRING_TRUE);
        assertFalse(goalsAndUserProps(ctxRefAbsent, singletonList(sonarGoal)).getKey().contains(sonarGoal));
    }

    @Test
    public void testAutoDisableJavadocAndSourceSkipIfNexus2Staging() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();
        ciOptionContext.getUserProperties().setProperty(PROP_NEXUS2_STAGING, BOOL_STRING_TRUE);

        final List<String> requestedGoals = asList(PHASE_SITE, PHASE_SITE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_JAVADOC_SKIP));
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_SOURCE_SKIP));
    }

    @Test
    public void testSkipSiteGoalsIfNotGenerateReports() {
        final CiOptionContext ciOptionContext = blankCiOptCtx();
        ciOptionContext.getUserProperties().setProperty(GENERATEREPORTS.getPropertyName(), BOOL_STRING_FALSE);

        final List<String> requestedGoals = asList(PHASE_CLEAN, PHASE_DEPLOY, PHASE_SITE, PHASE_SITE_DEPLOY);

        final Entry<List<String>, Properties> goalsAndProps = goalsAndUserProps(ciOptionContext, requestedGoals);
        final Collection<String> resultGoals = goalsAndProps.getKey();
        final Properties additionalUserProperties = goalsAndProps.getValue();

        assertTrue(resultGoals.contains(PHASE_CLEAN));
        assertTrue(resultGoals.contains(PHASE_DEPLOY));
        assertEquals(2, resultGoals.size());
        assertEquals(BOOL_STRING_FALSE, additionalUserProperties.getProperty(PROP_MAVEN_CLEAN_SKIP));
    }

    static CiOptionContext blankCiOptCtx() {
        final Properties systemProperties = new Properties();
        final Properties userProperties = new Properties();
        return new DefaultCiOptionContext(systemProperties, userProperties);
    }

    static Entry<List<String>, Properties> goalsAndUserProps(
        final CiOptionContext ciOptionContext,
        final List<String> requestedGoals
    ) {
        final MavenGoalEditor editor = MavenGoalEditor.newMavenGoalEditor(new LoggerSlf4jImpl(logger), ciOptionContext);
        final Entry<List<String>, Properties> goalsAndProps = editor.goalsAndUserProperties(ciOptionContext, requestedGoals);
        logger.info("requestedGoals: {}", requestedGoals);
        logger.info("resultGoals: {}", goalsAndProps.getKey());
        logger.info("additionalUserProperties: {}", goalsAndProps.getValue());
        return goalsAndProps;
    }
}
