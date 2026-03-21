package org.example.javawebdemo.task;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class StartupInitializer implements ApplicationRunner {
    private final UserService userService;
    private final MovieService movieService;

    public StartupInitializer(UserService userService, MovieService movieService) {
        this.userService = userService;
        this.movieService = movieService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureDefaultUsers();
        ensureDefaultMovies();
    }

    private void ensureDefaultMovies() {
        removeByTitle("初四脉冲");
        removeByTitle("初四脉冲：边界重启");

        List<Movie> existingMovies = movieService.listAll();
        Map<String, Movie> existingByTitle = new HashMap<>();
        for (Movie movie : existingMovies) {
            if (movie.getTitle() != null) {
                existingByTitle.put(movie.getTitle(), movie);
            }
        }

        for (MovieSeed seed : DEFAULT_MOVIES) {
            Movie existing = existingByTitle.get(seed.title());
            if (existing == null) {
                Movie movie = new Movie();
                movie.setTitle(seed.title());
                movie.setDirector(seed.director());
                movie.setActors(seed.actors());
                movie.setGenre(seed.genre());
                movie.setDurationMinutes(seed.durationMinutes());
                movie.setReleaseDate(seed.releaseDate());
                movie.setPosterUrl(seed.posterUrl());
                movie.setSynopsis(seed.synopsis());
                movie.setStatus(MovieStatus.ONLINE);
                movieService.create(movie);
                continue;
            }

            boolean changed = false;
            if (!Objects.equals(existing.getPosterUrl(), seed.posterUrl())) {
                existing.setPosterUrl(seed.posterUrl());
                changed = true;
            }
            if (!Objects.equals(existing.getStatus(), MovieStatus.ONLINE)) {
                existing.setStatus(MovieStatus.ONLINE);
                changed = true;
            }
            if (changed) {
                movieService.update(existing);
            }
        }
    }

    private void removeByTitle(String title) {
        movieService.listAll().stream()
                .filter(movie -> Objects.equals(movie.getTitle(), title))
                .map(Movie::getId)
                .forEach(movieService::delete);
    }

    private static final List<MovieSeed> DEFAULT_MOVIES = List.of(
            new MovieSeed("流浪地球2", "郭帆", "吴京, 刘德华", "科幻", 173,
                    LocalDate.of(2023, 1, 22),
                    "/css/png/img.png",
                    "太阳即将毁灭，人类启动流浪地球计划，展开跨世代太空生存行动。"),
            new MovieSeed("奥本海默", "克里斯托弗·诺兰", "基里安·墨菲, 艾米莉·布朗特", "剧情", 180,
                    LocalDate.of(2023, 8, 30),
                    "/css/png/img6.png",
                    "讲述原子弹之父奥本海默的科研突破、道德困境与时代抉择。"),
            new MovieSeed("沙丘2", "丹尼斯·维伦纽瓦", "提莫西·查拉梅, 赞达亚", "科幻", 166,
                    LocalDate.of(2024, 3, 8),
                    "/css/png/img5.png",
                    "保罗与弗雷曼人并肩作战，直面家族宿命与宇宙权力冲突。"),
            new MovieSeed("星际穿越", "克里斯托弗·诺兰", "马修·麦康纳, 安妮·海瑟薇", "科幻", 169,
                    LocalDate.of(2014, 11, 12),
                    "/css/png/img1.png",
                    "一支探险队穿越虫洞寻找新家园，在时间与亲情之间艰难前行。"),
            new MovieSeed("盗梦空间", "克里斯托弗·诺兰", "莱昂纳多·迪卡普里奥, 约瑟夫·高登-莱维特", "悬疑", 148,
                    LocalDate.of(2010, 9, 1),
                    "/css/png/img4.png",
                    "顶级窃梦团队执行一次反向植入任务，现实与梦境层层交错。"),
            new MovieSeed("银翼杀手2049", "丹尼斯·维伦纽瓦", "瑞恩·高斯林, 哈里森·福特", "科幻", 163,
                    LocalDate.of(2017, 10, 27),
                    "/css/png/img2.png",
                    "复制人警探追查旧案时发现足以颠覆秩序的惊人秘密。"),
            new MovieSeed("头号玩家", "史蒂文·斯皮尔伯格", "泰尔·谢里丹, 奥利维亚·库克", "冒险", 140,
                    LocalDate.of(2018, 3, 30),
                    "/css/png/img3.png",
                    "年轻玩家在虚拟世界中集结伙伴，争夺改变现实的终极钥匙。")
    );

    private record MovieSeed(
            String title,
            String director,
            String actors,
            String genre,
            Integer durationMinutes,
            LocalDate releaseDate,
            String posterUrl,
            String synopsis
    ) {
    }
}
