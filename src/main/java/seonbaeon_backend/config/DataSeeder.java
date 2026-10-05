package seonbaeon_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import seonbaeon_backend.entity.Review;
import seonbaeon_backend.entity.Teacher;
import seonbaeon_backend.entity.TeacherVideo;
import seonbaeon_backend.repository.ReviewRepository;
import seonbaeon_backend.repository.TeacherRepository;
import seonbaeon_backend.repository.TeacherVideoRepository;

import java.util.List;

@Configuration
public class DataSeeder {

    /*
     * These are starter/demo profiles for the MVP.
     *
     * IMPORTANT:
     * The demo videos below are public kids-English learning videos.
     * They are NOT recordings of the fictional teacher profiles.
     *
     * Replace these with verified teacher information and real teacher
     * demo videos before public production use.
     */
    private static final String DEMO_VIDEO_1 = "B-GRAj7HQ4Y";
    private static final String DEMO_VIDEO_2 = "gghDRJVxFxU";

    @Bean
    CommandLineRunner seedDatabase(
            TeacherRepository teacherRepository,
            ReviewRepository reviewRepository,
            TeacherVideoRepository teacherVideoRepository
    ) {
        return args -> {

            /*
             * Create starter teachers only when the database is empty.
             * Existing teachers will NOT be deleted or recreated.
             */
            repairExistingTeacherEncoding(teacherRepository);

            if (teacherRepository.count() == 0) {

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
                        "👩‍🏫"
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
                        "👩‍🏫"
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
                        "👩‍🏫"
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
                        "👨‍🏫"
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
                        "👩‍🏫"
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
                        "👩‍🏫"
                );

                jisoo.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Jisoo-Kim");
                hana.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Hana-Jung");
                minji.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Minji-Park");
                daniel.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Daniel-Choi");
                sora.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Sora-Lee");
                yuna.setProfileImageUrl("https://api.dicebear.com/9.x/personas/svg?seed=Yuna-Choi");

                List<Teacher> savedTeachers = teacherRepository.saveAll(
                        List.of(jisoo, hana, minji, daniel, sora, yuna)
                );            
            /*
             * Ensure existing teacher records have profile images.
             * Only blank image fields are updated.
             */
            List<Teacher> teachersNeedingImages = teacherRepository.findAll();

            for (Teacher teacher : teachersNeedingImages) {
                if (teacher.getProfileImageUrl() == null || teacher.getProfileImageUrl().isBlank()) {
                    String imageUrl = switch (teacher.getName()) {
                        case "Jisoo Kim" -> "https://api.dicebear.com/9.x/personas/svg?seed=Jisoo-Kim";
                        case "Hana Jung" -> "https://api.dicebear.com/9.x/personas/svg?seed=Hana-Jung";
                        case "Minji Park" -> "https://api.dicebear.com/9.x/personas/svg?seed=Minji-Park";
                        case "Daniel Choi" -> "https://api.dicebear.com/9.x/personas/svg?seed=Daniel-Choi";
                        case "Sora Lee" -> "https://api.dicebear.com/9.x/personas/svg?seed=Sora-Lee";
                        case "Yuna Choi" -> "https://api.dicebear.com/9.x/personas/svg?seed=Yuna-Choi";
                        default -> null;
                    };

                    if (imageUrl != null) {
                        teacher.setProfileImageUrl(imageUrl);
                    }
                }
            }

            teacherRepository.saveAll(teachersNeedingImages);
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
            }

            /*
             * Ensure existing teacher records have profile images.
             * Only blank image fields are updated.
             */
            List<Teacher> teachersNeedingImages = teacherRepository.findAll();

            for (Teacher teacher : teachersNeedingImages) {
                if (teacher.getProfileImageUrl() == null || teacher.getProfileImageUrl().isBlank()) {
                    String imageUrl = switch (teacher.getName()) {
                        case "Jisoo Kim" -> "https://api.dicebear.com/9.x/personas/svg?seed=Jisoo-Kim";
                        case "Hana Jung" -> "https://api.dicebear.com/9.x/personas/svg?seed=Hana-Jung";
                        case "Minji Park" -> "https://api.dicebear.com/9.x/personas/svg?seed=Minji-Park";
                        case "Daniel Choi" -> "https://api.dicebear.com/9.x/personas/svg?seed=Daniel-Choi";
                        case "Sora Lee" -> "https://api.dicebear.com/9.x/personas/svg?seed=Sora-Lee";
                        case "Yuna Choi" -> "https://api.dicebear.com/9.x/personas/svg?seed=Yuna-Choi";
                        default -> null;
                    };

                    if (imageUrl != null) {
                        teacher.setProfileImageUrl(imageUrl);
                    }
                }
            }

            teacherRepository.saveAll(teachersNeedingImages);
            /*
             * Ensure every teacher has at least two demo videos.
             *
             * IMPORTANT:
             * Existing video records are never deleted.
             * If a teacher already has one video, only one more is added.
             * If a teacher already has two or more, nothing is changed.
             */
            List<Teacher> teachers = teacherRepository.findAll();

            for (Teacher teacher : teachers) {

                List<TeacherVideo> existingVideos =
                        teacherVideoRepository.findByTeacherId(teacher.getId());

                int existingCount = existingVideos.size();

                if (existingCount >= 2) {
                    continue;
                }

                List<TeacherVideo> videosToAdd = new java.util.ArrayList<>();

                if (existingCount == 0) {
                    videosToAdd.add(createDemoVideo(
                            teacher,
                            "Lesson Introduction",
                            "A sample English lesson showing the teacher's teaching approach.",
                            DEMO_VIDEO_1
                    ));

                    videosToAdd.add(createDemoVideo(
                            teacher,
                            "Teaching Approach Demo",
                            "A second sample English lesson for families to understand the teaching style.",
                            DEMO_VIDEO_2
                    ));
                } else {
                    videosToAdd.add(createDemoVideo(
                            teacher,
                            "Teaching Approach Demo",
                            "A sample English lesson for families to understand the teaching style.",
                            DEMO_VIDEO_2
                    ));
                }

                teacherVideoRepository.saveAll(videosToAdd);
            }
        };
    }

    private void repairExistingTeacherEncoding(TeacherRepository teacherRepository) {
        var teachers = teacherRepository.findAll();

        for (Teacher teacher : teachers) {
            boolean changed = false;

            String qualification = teacher.getQualification();

            if (qualification != null) {
                String repaired = qualification
                        .replace(
                                "TESOL Certified \u00E2\u0080\u00A2 Early Childhood English Teaching",
                                "TESOL Certified \u2022 Early Childhood English Teaching"
                        )
                        .replace(
                                "CELTA \u00E2\u0080\u00A2 Young Learner English",
                                "CELTA \u2022 Young Learner English"
                        )
                        .replace(
                                "MA in Applied Linguistics \u00E2\u0080\u00A2 TESOL",
                                "MA in Applied Linguistics \u2022 TESOL"
                        )
                        .replace(
                                "TESOL Certified \u00E2\u0080\u00A2 School English Specialist",
                                "TESOL Certified \u2022 School English Specialist"
                        )
                        .replace(
                                "BA in English Education \u00E2\u0080\u00A2 TESOL",
                                "BA in English Education \u2022 TESOL"
                        );

                if (!repaired.equals(qualification)) {
                    teacher.setQualification(repaired);
                    changed = true;
                }
            }

            String emoji = teacher.getEmoji();

            if (emoji != null && emoji.contains("\u00F0")) {
                if ("Daniel Choi".equals(teacher.getName())) {
                    teacher.setEmoji("\uD83D\uDC68\u200D\uD83C\uDFEB");
                } else {
                    teacher.setEmoji("\uD83D\uDC69\u200D\uD83C\uDFEB");
                }
                changed = true;
            }

            if (changed) {
                teacherRepository.save(teacher);
            }
        }
    }
    private TeacherVideo createDemoVideo(
            Teacher teacher,
            String title,
            String description,
            String videoId
    ) {
        return new TeacherVideo(
                title,
                description,
                videoId,
                "youtube",
                null,
                teacher
        );
    }
}




