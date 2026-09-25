package seonbaeon_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import seonbaeon_backend.entity.Review;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.repository.ReviewRepository;
import seonbaeon_backend.repository.TeacherRepository;

import java.util.List;

@Configuration
public class DataSeeder {

    /*
     * These are starter/demo profiles for the MVP.
     * Replace the names, qualifications and demoVideoUrl values
     * with real teacher information before public production use.
     *
     * The demo videos below are public kids-English learning videos.
     * They are NOT recordings of the fictional teacher profiles.
     */
    private static final String DEMO_VIDEO_1 = "B-GRAj7HQ4Y";
    private static final String DEMO_VIDEO_2 = "gghDRJVxFxU";

    @Bean
    CommandLineRunner seedDatabase(
            TeacherRepository teacherRepository,
            ReviewRepository reviewRepository
    ) {
        return args -> {
            if (teacherRepository.count() > 0) {
                return;
            }

            Teacher jisoo = new Teacher(
                    "Jisoo Kim",
                    "Kids English",
                    "Busan",
                    "Kids English & Phonics",
                    "8+ Years",
                    250,
                    4.9,
                    128,
                    "Friendly & Interactive",
                    "TESOL Certified • Early Childhood English Teaching",
                    "A child-friendly English teacher who uses games, stories and simple speaking activities to help Korean children feel comfortable using English.",
                    DEMO_VIDEO_1,
                    "youtube",
                    true,
                    "👩🏻‍🏫"
            );

            Teacher hana = new Teacher(
                    "Hana Jung",
                    "Speaking",
                    "Seoul",
                    "Speaking & Conversation",
                    "6+ Years",
                    190,
                    4.8,
                    96,
                    "Conversation-based",
                    "CELTA • Young Learner English",
                    "Focuses on everyday English speaking, confidence and natural conversation for elementary and middle-school learners.",
                    DEMO_VIDEO_2,
                    "youtube",
                    true,
                    "👩🏻‍🏫"
            );

            Teacher minji = new Teacher(
                    "Minji Park",
                    "Reading & Writing",
                    "Busan",
                    "Reading, Writing & Vocabulary",
                    "7+ Years",
                    210,
                    4.9,
                    111,
                    "Patient & Supportive",
                    "MA in Applied Linguistics • TESOL",
                    "Helps young learners build vocabulary, reading habits and clear English writing step by step.",
                    DEMO_VIDEO_1,
                    "youtube",
                    true,
                    "👩🏻‍🏫"
            );

            Teacher daniel = new Teacher(
                    "Daniel Choi",
                    "Grammar",
                    "Daegu",
                    "Grammar & School English",
                    "5+ Years",
                    160,
                    4.7,
                    74,
                    "Clear & Step-by-step",
                    "TESOL Certified • School English Specialist",
                    "Explains grammar through simple examples and practice so students can connect classroom English with real communication.",
                    DEMO_VIDEO_2,
                    "youtube",
                    true,
                    "👨🏻‍🏫"
            );

            Teacher sora = new Teacher(
                    "Sora Lee",
                    "Storytelling",
                    "Busan",
                    "Storytelling & Vocabulary",
                    "5+ Years",
                    145,
                    4.8,
                    68,
                    "Fun & Creative",
                    "Young Learner English Certificate",
                    "Uses stories, pictures and speaking games to make English lessons engaging for younger children.",
                    DEMO_VIDEO_1,
                    "youtube",
                    true,
                    "👩🏻‍🏫"
            );

            Teacher yuna = new Teacher(
                    "Yuna Choi",
                    "School English",
                    "Seoul",
                    "Elementary School English",
                    "4+ Years",
                    120,
                    4.6,
                    52,
                    "Calm & Encouraging",
                    "BA in English Education • TESOL",
                    "Supports elementary learners with school English, vocabulary, pronunciation and homework-focused practice.",
                    DEMO_VIDEO_2,
                    "youtube",
                    true,
                    "👩🏻‍🏫"
            );

            List<Teacher> savedTeachers = teacherRepository.saveAll(
                    List.of(jisoo, hana, minji, daniel, sora, yuna)
            );

            reviewRepository.saveAll(List.of(
                    new Review(savedTeachers.get(0), "Minji", 5, "The class is easy to understand and my child enjoys the activities."),
                    new Review(savedTeachers.get(0), "Sujin", 5, "Very friendly teaching style. My child became more comfortable speaking English."),
                    new Review(savedTeachers.get(1), "Eunji", 5, "The speaking activities helped my child talk more confidently."),
                    new Review(savedTeachers.get(1), "Jiyoon", 4, "Clear explanations and lots of useful conversation practice."),
                    new Review(savedTeachers.get(2), "Mina", 5, "Reading became much easier after the lessons."),
                    new Review(savedTeachers.get(2), "Hyejin", 5, "Very patient and good at explaining new vocabulary."),
                    new Review(savedTeachers.get(3), "Jun", 5, "Grammar is explained in a simple way."),
                    new Review(savedTeachers.get(3), "Ara", 4, "Good school-English practice and clear examples."),
                    new Review(savedTeachers.get(4), "Sumin", 5, "My child loves the stories and games."),
                    new Review(savedTeachers.get(4), "Yerin", 5, "Fun class and good vocabulary practice."),
                    new Review(savedTeachers.get(5), "Dami", 5, "Calm and encouraging teacher."),
                    new Review(savedTeachers.get(5), "Nari", 4, "Helpful for elementary school English.")
            ));
        };
    }
}
