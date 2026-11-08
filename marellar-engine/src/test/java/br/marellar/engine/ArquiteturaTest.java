package br.marellar.engine;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Regras de arquitetura (README 10 e 11):
 * dinheiro nunca em double/float e o motor nao depende de Spring, banco ou Godot.
 */
class ArquiteturaTest {

    private static final JavaClasses MOTOR = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.marellar.engine");

    @Test
    void nenhumCampoDoMotorEDoubleOuFloat() {
        noFields().should().haveRawType(double.class)
                .orShould().haveRawType(Double.class)
                .orShould().haveRawType(float.class)
                .orShould().haveRawType(Float.class)
                .because("dinheiro e quantidades do jogo usam BigDecimal (README 2)")
                .check(MOTOR);
    }

    @Test
    void nenhumMetodoDoMotorDevolveDoubleOuFloat() {
        noMethods().should().haveRawReturnType(double.class)
                .orShould().haveRawReturnType(Double.class)
                .orShould().haveRawReturnType(float.class)
                .orShould().haveRawReturnType(Float.class)
                .because("dinheiro e quantidades do jogo usam BigDecimal (README 2)")
                .check(MOTOR);
    }

    @Test
    void motorNaoDependeDeFramework() {
        noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "javax.persistence..", "java.sql..")
                .because("o marellar-engine e simulacao pura (README 11)")
                .check(MOTOR);
    }
}
