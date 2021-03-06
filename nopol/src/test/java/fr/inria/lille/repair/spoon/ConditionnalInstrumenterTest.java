package fr.inria.lille.repair.spoon;

import fr.inria.lille.commons.spoon.SpoonedClass;
import fr.inria.lille.commons.spoon.SpoonedProject;
import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.synth.StatementTypeDetector;
import fr.inria.lille.repair.nopol.spoon.NopolProcessor;
import fr.inria.lille.repair.nopol.spoon.brutpol.ConditionalInstrumenter;
import fr.inria.lille.repair.nopol.spoon.smt.ConditionalReplacer;
import org.junit.Test;
import spoon.Launcher;
import spoon.processing.Processor;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;
import xxl.java.compiler.DynamicCompilationException;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by bdanglot on 9/29/16.
 */
public class ConditionnalInstrumenterTest {

	@Test
	public void testConditionnalInstrumenter() throws Exception {
		Config config = new Config();
		config.setSynthesis(Config.NopolSynthesis.BRUTPOL);
		File fileClassToSpoon = new File("src/test/resources/spoon/example/Thaliana.java");
		File[] sourceFiles = {fileClassToSpoon};
		SpoonedProject spooner = new SpoonedProject(sourceFiles
				, new URL[]{fileClassToSpoon.toURI().toURL()}, config);

		Launcher l = new Launcher();
		l.addInputResource("src/test/resources/spoon/example/Thaliana.java");
		l.buildModel();
		CtIf ifStatement = l.getFactory().Class().get("spoon.example.Thaliana").getElements(new TypeFilter<CtIf>(CtIf.class) {
			@Override
			public boolean matches(CtIf element) {
				return "method".equals(((CtMethod) element.getParent(CtMethod.class)).getSimpleName());
			}
		}).get(0);

		SpoonedClass spoonCl = spooner.forked("spoon.example.Thaliana");
		StatementTypeDetector detector = new StatementTypeDetector(spoonCl.getSimpleType().getPosition().getFile(), ifStatement.getPosition().getLine(), config.getType());
		spoonCl.process(detector);
		NopolProcessor nopolProcessor = new ConditionalReplacer(detector.statement());
		Processor<CtStatement> processor = new ConditionalInstrumenter(nopolProcessor, config.getType().getType());
		spoonCl.process(processor);

		CtType<Object> spoonThaliana = spoonCl.spoonFactory().Type().get("spoon.example.Thaliana");

		assertTrue(!(spoonThaliana.getElements(new TypeFilter<CtTry>(CtTry.class) {
			@Override
			public boolean matches(CtTry element) {
				return true;
			}
		}).isEmpty()));

		assertEquals("__NopolProcessorException", spoonThaliana.getElements(new TypeFilter<CtCatch>(CtCatch.class) {
			@Override
			public boolean matches(CtCatch element) {
				return true;
			}
		}).get(0).getParameter().getSimpleName());

		ifStatement = l.getFactory().Class().get("spoon.example.Thaliana").getElements(new TypeFilter<CtIf>(CtIf.class) {
			@Override
			public boolean matches(CtIf element) {
				return "throwingExceptionDueToTheName".equals(((CtMethod) element.getParent(CtMethod.class)).getSimpleName());
			}
		}).get(0);

		spoonCl = spooner.forked("spoon.example.Thaliana");
		detector = new StatementTypeDetector(spoonCl.getSimpleType().getPosition().getFile(), ifStatement.getPosition().getLine(), config.getType());
		spoonCl.process(detector);
		nopolProcessor = new ConditionalReplacer(detector.statement());
		processor = new ConditionalInstrumenter(nopolProcessor, config.getType().getType());
		try {
			spoonCl.process(processor);
			fail();
		} catch (DynamicCompilationException exception) {
			assertEquals("Aborting: dynamic compilation failed", exception.getMessage());
		}

	}
}
