/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.jumpingbean.gc.test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javax.management.MalformedObjectNameException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import org.junit.Test;
import za.co.jumpingbean.gc.service.GeneratorService;
import za.co.jumpingbean.gc.service.JMXQueryRunner;
import za.co.jumpingbean.gc.service.constants.DESC;
import za.co.jumpingbean.gc.service.constants.EdenSpace;
import za.co.jumpingbean.gc.service.constants.OldGenerationCollector;
import za.co.jumpingbean.gc.service.constants.OldGenerationSpace;
import za.co.jumpingbean.gc.service.constants.PermGen;
import za.co.jumpingbean.gc.service.constants.SurvivorSpace;
import za.co.jumpingbean.gc.service.constants.YoungGenerationCollector;

/**
 *
 * @author mark
 */
public class GCGeneratorTest {

    @Test
    public void testJMXQueryBrowser() {

    }

    @Test
    public void testSerialGCLookups() {

        assertThat(EdenSpace.isMember("Eden Space"), is(true));
        assertThat(SurvivorSpace.isMember("Survivor Space"), is(true));
        assertThat(OldGenerationSpace.isMember("Tenured Gen"), is(true));

        //JVMCollector serial = JVMCollector.SERIALGC;
        assertThat(YoungGenerationCollector.isMember("Copy"), is(true));
        assertThat(OldGenerationCollector.isMember("MarkSweepCompact"), is(true));
    }

    @Test
    public void testParallelGCLookups() throws IllegalStateException {

        assertThat(EdenSpace.isMember("PS Eden Space"), is(true));
        assertThat(SurvivorSpace.isMember("PS Survivor Space"), is(true));
        assertThat(OldGenerationSpace.isMember("PS Old Gen"), is(true));

        //JVMCollector parallelGC = JVMCollector.PARALLELGC;
        assertThat(YoungGenerationCollector.isMember("PS Scavenge"), is(true));
        assertThat(OldGenerationCollector.isMember("PS MarkSweep"), is(true));

    }

    @Test
    public void testConcMarkSweepGCLookups() throws IllegalStateException {
        assertThat(EdenSpace.isMember("Par Eden Space"), is(true));
        assertThat(SurvivorSpace.isMember("Par Survivor Space"), is(true));
        assertThat(OldGenerationSpace.isMember("CMS Old Gen"), is(true));

        //JVMCollector concMarkSweepGC = JVMCollector.CONCMARKSWEEP;
        assertThat(YoungGenerationCollector.isMember("ParNew"), is(true));
        assertThat(OldGenerationCollector.isMember("ConcurrentMarkSweep"), is(true));

    }

    @Test
    public void testPermGenLookup() throws IllegalStateException {
        assertThat(PermGen.isMember("Perm Gen"), is(true));
        assertThat(PermGen.isMember("PS Perm Gen"), is(true));
        assertThat(PermGen.isMember("CMS Perm Gen"), is(true));
        assertThat(PermGen.isMember("G1 Perm Gen"), is(true));
        assertThat(PermGen.isMember("Metaspace"), is(true));
    }

    @Test
    public void testG1GCLookups() throws IllegalStateException {
        assertThat(EdenSpace.isMember("G1 Eden Space"), is(true));
        assertThat(SurvivorSpace.isMember("G1 Survivor Space"), is(true));
        assertThat(OldGenerationSpace.isMember("G1 Old Gen"), is(true));

        //JVMCollector concMarkSweepGC = JVMCollector.G1GC;
        assertThat(YoungGenerationCollector.isMember("G1 Young Generation"), is(true));
        assertThat(OldGenerationCollector.isMember("G1 Old Generation"), is(true));
    }

    @Test
    public void testStartStopTestApp() throws CannotCompileException, IOException, NotFoundException,
            InterruptedException, MalformedObjectNameException, IllegalStateException {
        createTmpClassMainClass();
        GeneratorService gen = new GeneratorService();
        UUID id = null;
        try {

            List<String> cmdOption = new LinkedList<>();
            cmdOption.add("-XX:+UseConcMarkSweepGC");
            id = gen.startTestApp("8282", "", "Test", cmdOption);
            String output = gen.getProcessOutput(id);
            assertThat(output, is(equalTo("Hello World")));
        } finally {
            if (id != null) {
                gen.stopTestApp(id);
            }
        }
    }

    @Test
    public void testJMXQueryRunnerConcMarkSweepGCQuery() throws IOException, CannotCompileException, NotFoundException, MalformedObjectNameException {
        createTmpClassMainClass();
        GeneratorService gen = new GeneratorService();
        List<String> cmdOption = new LinkedList<>();
        cmdOption.add("-XX:+UseConcMarkSweepGC");
        UUID procId = null;
        try {

            procId = gen.startTestApp("8282", "", "Test", cmdOption);
            JMXQueryRunner runner = gen.getJMXQueryRunner(procId);

            //runner.init();
            assertThat(runner.getOldGenCollector(), is(notNullValue()));
            assertThat(runner.getYoungGenCollector(), is(notNullValue()));
            assertThat(runner.getEdenSpace(), is(notNullValue()));
            assertThat(runner.getPermGenSpace(), is(notNullValue()));
            assertThat(runner.getSurvivorSpace(), is(notNullValue()));
            assertThat(runner.getOldGenSpace(), is(notNullValue()));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEUSED).longValue(),
                    is(Matchers.greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));
        } finally {
            if (procId != null) {
                gen.stopTestApp(procId);
            }
        }
    }

    @Test
    public void testJMXQueryRunnerSerialGCQuery() throws IOException, CannotCompileException, NotFoundException, MalformedObjectNameException {
        createTmpClassMainClass();
        GeneratorService gen = new GeneratorService();
        UUID procId = null;
        try {
            List<String> cmdOption = new LinkedList<>();
            cmdOption.add("-XX:+UseSerialGC");
            procId = gen.startTestApp("8282", "", "Test", cmdOption);
            JMXQueryRunner runner = gen.getJMXQueryRunner(procId);

            //runner.init();
            assertThat(runner.getOldGenCollector(), is(notNullValue()));
            assertThat(runner.getYoungGenCollector(), is(notNullValue()));
            assertThat(runner.getEdenSpace(), is(notNullValue()));
            assertThat(runner.getPermGenSpace(), is(notNullValue()));
            assertThat(runner.getSurvivorSpace(), is(notNullValue()));
            assertThat(runner.getOldGenSpace(), is(notNullValue()));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEUSED).longValue(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));
        } finally {
            if (procId != null) {
                gen.stopTestApp(procId);
            }
        }
    }

    @Test
    public void testJMXQueryRunnerParallelGCQuery() throws IOException, CannotCompileException,
            NotFoundException, MalformedObjectNameException, IllegalStateException {
        createTmpClassMainClass();
        GeneratorService gen = new GeneratorService();
        UUID procId = null;
        try {

            List<String> cmdOption = new LinkedList<>();
            cmdOption.add("-XX:+UseParallelGC");
            procId = gen.startTestApp("8282", "", "Test", cmdOption);
            JMXQueryRunner runner = gen.getJMXQueryRunner(procId);

            //runner.init();
            assertThat(runner.getOldGenCollector(), is(notNullValue()));
            assertThat(runner.getYoungGenCollector(), is(notNullValue()));
            assertThat(runner.getEdenSpace(), is(notNullValue()));
            assertThat(runner.getPermGenSpace(), is(notNullValue()));
            assertThat(runner.getSurvivorSpace(), is(notNullValue()));
            assertThat(runner.getOldGenSpace(), is(notNullValue()));

            assertThat(gen.getMeasure(procId, DESC.EDENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEUSED).longValue(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEFREE).longValue(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));
        } finally {
            if (procId != null) {
                gen.stopTestApp(procId);
            }
        }
    }

    @Test
    public void testJMXQueryRunnerG1GCQuery() throws IOException, CannotCompileException, NotFoundException, MalformedObjectNameException {
        createTmpClassMainClass();
        GeneratorService gen = new GeneratorService();
        UUID procId = null;
        try {
            List<String> cmdOption = new LinkedList<>();
            cmdOption.add("-XX:+UseG1GC");
            procId = gen.startTestApp("8282", "", "Test", cmdOption);
            JMXQueryRunner runner = gen.getJMXQueryRunner(procId);

            //runner.init();
            assertThat(runner.getOldGenCollector(), is(notNullValue()));
            assertThat(runner.getYoungGenCollector(), is(notNullValue()));
            assertThat(runner.getEdenSpace(), is(notNullValue()));
            assertThat(runner.getPermGenSpace(), is(notNullValue()));
            assertThat(runner.getSurvivorSpace(), is(notNullValue()));
            assertThat(runner.getOldGenSpace(), is(notNullValue()));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.EDENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.PERMGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEMAX).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEUSED).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACEFREE).longValue(),
                    is(greaterThan(0L)));
            assertThat(gen.getMeasure(procId, DESC.OLDGENSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));

            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEMAX).longValue(),
                    is(greaterThanOrEqualTo(-1L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEUSED).longValue(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACEFREE).longValue(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat(gen.getMeasure(procId, DESC.SURVIVORSPACECOMMITTED).longValue(),
                    is(greaterThan(0L)));
        } finally {
            if (procId != null) {
                gen.stopTestApp(procId);
            }
        }
    }

    private void createTmpClassMainClass() throws CannotCompileException, NotFoundException, IOException {
        ClassPool cpool = ClassPool.getDefault();
        CtClass tmpClass;
        try {
            tmpClass = cpool.get("Test");
        } catch (NotFoundException ex) {
            tmpClass = cpool.makeClass("Test");
            if (tmpClass.isFrozen()) {
                return;
            }
            CtConstructor defaultConstructor = CtNewConstructor.defaultConstructor(tmpClass);
            tmpClass.addConstructor(defaultConstructor);
            String str = "public static void main(String []args){"
                    + "Test test = new Test();"
                    + "Byte[]bigArr = new Byte[100000000];"
                    + "Byte[]arr = new Byte[1000];"
                    + "System.out.println(\"Hello World\");"
                    + "try {"
                    + "    Thread.sleep(1000L);"
                    + "} catch (InterruptedException ex1) {"
                    + "}"
                    + "}";
            CtMethod main = CtNewMethod.make(str, tmpClass);
            tmpClass.addMethod(main);
            tmpClass.writeFile();
        }
    }
}