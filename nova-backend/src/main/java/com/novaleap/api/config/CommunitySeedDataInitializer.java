package com.novaleap.api.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.NoteComment;
import com.novaleap.api.entity.NoteLike;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.User;
import com.novaleap.api.entity.UserQuestionMastery;
import com.novaleap.api.entity.Wish;
import com.novaleap.api.entity.WishComment;
import com.novaleap.api.entity.WishLike;
import com.novaleap.api.mapper.NoteCommentMapper;
import com.novaleap.api.mapper.NoteLikeMapper;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.mapper.UserQuestionMasteryMapper;
import com.novaleap.api.mapper.WishCommentMapper;
import com.novaleap.api.mapper.WishLikeMapper;
import com.novaleap.api.mapper.WishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "nova.startup.community-seed",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CommunitySeedDataInitializer implements ApplicationRunner {

    private static final String Q_COUNT_KEY = "nova:leaderboard:question_done";
    private static final String Q_SET_PREFIX = "nova:leaderboard:qset:";
    private static final String GAME_BEST_KEY = "nova:leaderboard:game_best";
    private static final String WISH_COUNT_KEY = "nova:leaderboard:wish_count";
    private static final String PROFILE_AVATAR_KEY_PREFIX = "nova:profile:avatar:";

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("seed_joy_offline", "\u5feb\u4e50\u4e0d\u5728\u670d\u52a1\u533a", "\ud83e\udd73", 18),
            new SeedUser("seed_cloudy_today", "\u5929\u6c14\u8f6c\u9634\u4e0d\u8f6c\u6674", "\ud83d\ude03", 17),
            new SeedUser("seed_too_quiet", "\u6700\u8fd1\u5e38\u5e38\u592a\u5b89\u9759", "\ud83e\udd17", 16),
            new SeedUser("seed_low_pressure", "\u4f4e\u6c14\u538b\u533a", "\ud83d\ude05", 15),
            new SeedUser("seed_mood_overcast", "\u60c5\u7eea\u9634\u5929", "\ud83d\ude09", 14),
            new SeedUser("seed_late_happy", "\u665a\u70b9\u5f00\u5fc3", "\ud83e\udee1", 13),
            new SeedUser("seed_happy_loading", "\u5f00\u5fc3\u52a0\u8f7d\u4e2d", "\ud83d\ude0a", 12),
            new SeedUser("seed_human_observer", "\u4eba\u95f4\u89c2\u5bdf\u5458", "\ud83d\ude0f", 11),
            new SeedUser("seed_silent_mode", "\u95f4\u6b47\u6027\u6c89\u9ed8", "\ud83e\udd29", 10)
    );

    private static final List<NoteSeed> NOTE_SEEDS = List.of(
            new NoteSeed(
                    "seed_joy_offline",
                    "\u4e00\u9762\u6302\u5728\u9879\u76ee\u6df1\u6326\uff0c\u6211\u624d\u53d1\u73b0\u81ea\u5df1\u4e00\u76f4\u5728\u80cc\u201c\u5047\u9879\u76ee\u201d",
                    "\u9762\u7ecf\u590d\u76d8",
                    "\ud83d\udcbc",
                    128,
                    9,
                    "\u4eca\u5929\u590d\u76d8\u4e86\u4e00\u573a\u4e2d\u5382\u540e\u7aef\u4e00\u9762\uff0c\u672c\u6765\u4ee5\u4e3a\u4f1a\u95ee Redis \u548c JVM\uff0c\u7ed3\u679c\u524d 30 \u5206\u949f\u90fd\u5728\u8ffd\u7740\u9879\u76ee\u95ee\u3002",
                    "## \u4eca\u5929\u6302\u7684\u70b9\n\n\u4e00\u9762\u9762\u8bd5\u5b98\u6ca1\u6709\u5148\u95ee\u516b\u80a1\uff0c\u76f4\u63a5\u8ba9\u6211\u8bb2\u7b80\u5386\u4e0a\u90a3\u4e2a\u201c\u63a5\u53e3\u6027\u80fd\u4f18\u5316\u201d\u7684\u9879\u76ee\u3002\u6211\u4e00\u5f00\u53e3\u5c31\u5728\u8bf4\u7528\u4e86 Redis\uff0c\u7528\u4e86 MQ\uff0c\u7528\u4e86\u5f02\u6b65\uff0c\u4f46\u5bf9\u9762\u4e00\u76f4\u8ffd\u95ee\uff1a\u201c\u4e3a\u4ec0\u4e48\u4e00\u5b9a\u8981\u52a0 Redis\uff1f\u201d\u201c\u70ed\u70b9\u662f\u600e\u4e48\u51fa\u6765\u7684\uff1f\u201d\u201c\u5982\u679c\u7f13\u5b58\u96ea\u5d29\uff0c\u4f60\u8fd9\u4e2a\u670d\u52a1\u6297\u4e0d\u6297\u5f97\u4f4f\uff1f\u201d\n\n\u6211\u5f53\u65f6\u7684\u95ee\u9898\u4e0d\u662f\u4e0d\u4f1a\uff0c\u800c\u662f\u8bf4\u5f97\u592a\u50cf\u80cc\u7b54\u6848\u3002\u6211\u4f1a\u8bf4\u201c\u7f13\u5b58\u51fb\u7a7f\u7528\u5e03\u9686\u8fc7\u6ee4\u5668\uff0c\u96ea\u5d29\u52a0\u968f\u673a\u8fc7\u671f\u65f6\u95f4\u201d\uff0c\u4f46\u8bf4\u4e0d\u51fa\u6211\u8fd9\u4e2a\u9879\u76ee\u91cc\u5230\u5e95\u54ea\u4e2a key \u662f\u70ed\u70b9\uff0cQPS \u5927\u6982\u5230\u4ec0\u4e48\u91cf\u7ea7\uff0c\u6709\u6ca1\u6709\u771f\u9047\u5230\u8fc7\u8d85\u65f6\u3002\n\n## \u6211\u665a\u4e0a\u8865\u7684\u4e1c\u897f\n\n\u6211\u73b0\u5728\u628a\u6bcf\u4e2a\u9879\u76ee\u90fd\u91cd\u65b0\u62c6\u6210 5 \u683c\uff1a\n\n- \u4e1a\u52a1\u80cc\u666f\u662f\u4ec0\u4e48\n- \u6570\u636e\u91cf\u5927\u6982\u591a\u5c11\n- \u4e3a\u4ec0\u4e48\u9009\u8fd9\u4e2a\u65b9\u6848\n- \u771f\u5b9e\u8e29\u8fc7\u4ec0\u4e48\u5751\n- \u6700\u540e\u6307\u6807\u6539\u5584\u4e86\u591a\u5c11\n\n\u611f\u89c9\u4e4b\u524d\u90a3\u79cd\u5199\u6cd5\u66f4\u50cf\u201c\u6211\u53c2\u4e0e\u8fc7\u9879\u76ee\u201d\uff0c\u4e0d\u50cf\u201c\u6211\u771f\u7684\u628a\u8fd9\u4e2a\u9879\u76ee\u505a\u8fc7\u201d\u3002\u4e0b\u6b21\u518d\u88ab\u95ee\u201c\u5982\u679c\u518d\u6765\u4e00\u6b21\u4f60\u4f1a\u600e\u4e48\u8bbe\u8ba1\u201d\uff0c\u81f3\u5c11\u4e0d\u4f1a\u518d\u76f4\u63a5\u5361\u4f4f\u3002"
            ),
            new NoteSeed(
                    "seed_cloudy_today",
                    "\u4e8c\u9762\u95ee\u5230 MySQL \u7d22\u5f15\u5931\u6548\uff0c\u6211\u7ec8\u4e8e\u77e5\u9053\u8fd9\u9898\u8be5\u600e\u4e48\u7b54",
                    "\u9762\u7ecf\u590d\u76d8",
                    "\ud83d\udcdd",
                    116,
                    8,
                    "\u4e0d\u662f\u80cc\u51fa\u201c\u6700\u5de6\u524d\u7f00\u201d\u5c31\u7ed3\u675f\uff0c\u9762\u8bd5\u5b98\u66f4\u60f3\u542c\u4f60\u80fd\u4e0d\u80fd\u628a SQL \u548c\u6267\u884c\u8ba1\u5212\u4e32\u8d77\u6765\u3002",
                    "## \u4e8c\u9762\u8ffd\u95ee\u5f97\u7279\u522b\u7ec6\n\n\u4eca\u5929\u88ab\u95ee\u5230\u4e00\u9898\uff1a\u201c\u8054\u5408\u7d22\u5f15(a, b, c)\uff0c\u4e3a\u4ec0\u4e48 where a = ? and c = ? \u4e0d\u4e00\u5b9a\u8d70\u5b8c\u6574\u7d22\u5f15\uff1f\u201d\u6211\u4e00\u5f00\u59cb\u4e0a\u6765\u5c31\u80cc\u6700\u5de6\u524d\u7f00\uff0c\u5bf9\u9762\u76f4\u63a5\u8bf4\uff1a\u201c\u8fd9\u4e9b\u6211\u77e5\u9053\uff0c\u4f60\u6309 optimizer \u7684\u89c6\u89d2\u8bb2\u3002\u201d\n\n\u90a3\u4e00\u523b\u6211\u624d\u53d1\u73b0\uff0c\u6211\u4e4b\u524d\u90fd\u662f\u5728\u80cc\u7b54\u6848\uff0c\u4e0d\u662f\u5728\u7406\u89e3 SQL \u600e\u4e48\u8dd1\u3002\u540e\u9762\u6211\u56de\u53bb\u628a\u5e38\u89c1\u7684\u51e0\u79cd\u60c5\u51b5\u91cd\u65b0\u8bb0\u4e86\u4e00\u904d\uff1a\n\n- \u7b49\u503c\u5339\u914d\u662f\u5426\u8fde\u7eed\n- \u8303\u56f4\u67e5\u8be2\u5728\u54ea\u4e00\u5217\u622a\u65ad\n- like \u4ee5\u524d\u7f00\u5f00\u5934\u8fd8\u662f\u901a\u914d\u5f00\u5934\n- \u6709\u6ca1\u6709\u53d1\u751f\u9690\u5f0f\u7c7b\u578b\u8f6c\u6362\n- \u6700\u7ec8\u8fd8\u662f\u8981\u770b explain\uff0c\u4e0d\u662f\u770b\u611f\u89c9\n\n## \u6211\u73b0\u5728\u7684\u8bb0\u6cd5\n\n\u6211\u7ed9\u81ea\u5df1\u5b9a\u4e86\u4e2a\u89c4\u77e9\uff1a\u6bcf\u9047\u5230\u4e00\u4e2a\u516b\u80a1\u9898\uff0c\u81f3\u5c11\u5199\u4e00\u6761\u771f\u5b9e SQL \u4f8b\u5b50\uff0c\u518d\u9644\u4e00\u5f20 explain \u622a\u56fe\u601d\u8def\u3002\u8981\u4e0d\u7136\u5230\u9762\u8bd5\u91cc\u4e00\u88ab\u8ffd\u95ee\u201c\u90a3\u4f60\u7ebf\u4e0a\u600e\u4e48\u5b9a\u4f4d\u7684\uff1f\u201d\uff0c\u8fd8\u662f\u53ea\u80fd\u9ed8\u4f4f\u3002"
            ),
            new NoteSeed(
                    "seed_too_quiet",
                    "\u516b\u80a1\u80cc\u5230\u7b2c\u4e94\u8f6e\uff0c\u6211\u5f00\u59cb\u7528\u9879\u76ee\u4f8b\u5b50\u6551\u81ea\u5df1",
                    "\u5b66\u4e60\u65b9\u6cd5",
                    "\ud83e\udde9",
                    103,
                    7,
                    "\u5355\u7eaf\u80cc\u7b54\u6848\u5f88\u5bb9\u6613\u5fd8\uff0c\u4f46\u628a\u6bcf\u4e2a\u77e5\u8bc6\u70b9\u548c\u81ea\u5df1\u505a\u8fc7\u7684\u9879\u76ee\u7ed1\u8d77\u6765\u4e4b\u540e\uff0c\u56de\u7b54\u53cd\u800c\u987a\u4e86\u3002",
                    "## \u4e4b\u524d\u7684\u72b6\u6001\n\n\u524d\u9635\u5b50\u6211\u4e00\u76f4\u5728\u80cc Java \u57fa\u7840\u3001JVM\u3001Redis\u3001Spring \u90a3\u4e9b\u9ad8\u9891\u9898\uff0c\u80cc\u5230\u540e\u9762\u6709\u70b9\u9ebb\u6728\u4e86\u3002\u4f8b\u5982\u201c\u7ebf\u7a0b\u6c60\u62d2\u7edd\u7b56\u7565\u6709\u54ea\u4e9b\u201d\u6211\u80fd\u7acb\u9a6c\u8bf4\u51fa\u6765\uff0c\u4f46\u4f60\u771f\u95ee\u6211\u9879\u76ee\u91cc\u4e3a\u4ec0\u4e48\u6838\u5fc3\u7ebf\u7a0b\u6570\u8bbe 8\uff0c\u6211\u53c8\u5f00\u59cb\u98d8\u3002\n\n## \u540e\u6765\u7684\u6539\u6cd5\n\n\u6211\u5f00\u59cb\u7528\u201c\u77e5\u8bc6\u70b9 -> \u9879\u76ee\u573a\u666f -> \u8ffd\u95ee\u95ee\u9898\u201d\u8fd9\u79cd\u65b9\u5f0f\u8bb0\u7b14\u8bb0\u3002\u6bd4\u5982\uff1a\n\n- Redis \u8fc7\u671f\u7b56\u7565 -> \u77ed\u4fe1\u9a8c\u8bc1\u7801\u548c\u9996\u9875\u70ed\u95e8\u63a8\u8350\u600e\u4e48\u8bbe\u7f6e TTL\n- \u7ebf\u7a0b\u6c60 -> \u5f02\u6b65\u53d1\u90ae\u4ef6\u4e3a\u4ec0\u4e48\u4e0d\u76f4\u63a5 new Thread\n- \u4e8b\u52a1\u5931\u6548 -> \u6211\u4eec\u9879\u76ee\u91cc\u4e3a\u4ec0\u4e48\u6709\u4e00\u4e2a\u65b9\u6cd5\u660e\u660e\u52a0\u4e86 @Transactional \u8fd8\u662f\u6ca1\u56de\u6eda\n\n\u8fd9\u6837\u6574\u7406\u540e\uff0c\u9762\u8bd5\u7684\u65f6\u5019\u81f3\u5c11\u4e0d\u4f1a\u53ea\u6709\u201c\u6807\u51c6\u7b54\u6848\u201d\u3002\u73b0\u5728\u6211\u4e00\u5f20\u5361\u7247\u4e0a\u53ea\u8bb0\u4e00\u4e2a\u6838\u5fc3\u77e5\u8bc6\u70b9\u3001\u4e00\u4e2a\u9879\u76ee\u573a\u666f\u3001\u4e24\u4e2a\u53ef\u80fd\u7684\u8ffd\u95ee\uff0c\u6548\u7387\u6bd4\u4e4b\u524d\u9ad8\u5f88\u591a\u3002"
            ),
            new NoteSeed(
                    "seed_low_pressure",
                    "\u6295\u9012\u522b\u8d2a\u591a\uff0c\u6211\u628a\u6bcf\u5929\u76ee\u6807\u6539\u6210\u4e862\u6295 + 1 \u590d\u76d8",
                    "\u6c42\u804c\u8bb0\u5f55",
                    "\ud83e\ude81",
                    94,
                    6,
                    "\u771f\u7684\u4e0d\u662f\u6295\u5f97\u8d8a\u591a\u5c31\u8d8a\u5b89\u5fc3\uff0c\u6211\u524d\u671f\u4e00\u5929\u4e71\u6295 20 \u591a\u5bb6\uff0c\u540e\u9762\u53cd\u800c\u6ca1\u7cbe\u529b\u8ddf\u8fdb\u3002",
                    "\u6700\u8fd1\u628a\u6295\u9012\u8282\u594f\u6539\u4e86\u4e00\u4e0b\uff0c\u53ea\u7ed9\u81ea\u5df1\u5b9a 3 \u4ef6\u4e8b\uff1a\n\n- \u7cbe\u6295 2 \u4e2a\u5c97\u4f4d\n- \u8ba4\u771f\u6539 1 \u6b21\u7b80\u5386\n- \u590d\u76d8 1 \u573a\u9762\u8bd5\u6216 1 \u4efd\u7b14\u8bb0\n\n\u4e4b\u524d\u6211\u4e00\u76f4\u89c9\u5f97\u6ca1 offer \u5c31\u8981\u66b4\u529b\u591a\u6295\uff0c\u4f46\u540e\u6765\u53d1\u73b0\u95ee\u9898\u6839\u672c\u4e0d\u5728\u201c\u6295\u5f97\u4e0d\u591f\u591a\u201d\uff0c\u800c\u662f\u6bcf\u4e2a JD \u6211\u90fd\u6ca1\u6709\u5bf9\u7740\u6539\u3002\u6709\u4e9b\u5c97\u4f4d\u8981\u7684\u662f Java \u57fa\u7840\u548c MySQL\uff0c\u6709\u4e9b\u66f4\u770b\u9879\u76ee\u91cc\u7684\u4e2d\u95f4\u4ef6\u548c\u5de5\u7a0b\u5316\uff0c\u7ed3\u679c\u6211\u7b80\u5386\u5199\u5f97\u90fd\u4e00\u6837\u3002\n\n\u73b0\u5728\u6211\u6bcf\u6295\u4e00\u4e2a\u5c97\uff0c\u90fd\u4f1a\u987a\u624b\u628a\u7b80\u5386\u4e0a\u7684 1 \u6761\u9879\u76ee\u63cf\u8ff0\u91cd\u5199\u5f97\u66f4\u8d34 JD\u3002\u6570\u91cf\u5c11\u4e86\uff0c\u4f46\u5fc3\u91cc\u6ca1\u90a3\u4e48\u4e71\u4e86\uff0c\u7ea6\u9762\u7387\u53cd\u800c\u6bd4\u4e4b\u524d\u597d\u4e00\u70b9\u3002"
            ),
            new NoteSeed(
                    "seed_mood_overcast",
                    "\u7b80\u5386\u4e0a\u90a3\u53e5\u201c\u8d1f\u8d23\u4f18\u5316\u63a5\u53e3\u6027\u80fd\u201d\uff0c\u4eca\u5929\u7ec8\u4e8e\u88ab\u6211\u6539\u5177\u4f53\u4e86",
                    "\u7b80\u5386\u5de5\u574a",
                    "\u270d\ufe0f",
                    88,
                    5,
                    "\u6ca1\u6709\u6570\u5b57\u3001\u6ca1\u6709\u573a\u666f\u3001\u6ca1\u6709\u524d\u540e\u5bf9\u6bd4\u7684\u7b80\u5386\u63cf\u8ff0\uff0c\u770b\u8d77\u6765\u5c31\u50cf\u6ca1\u505a\u8fc7\u3002",
                    "## \u6211\u539f\u6765\u7684\u5199\u6cd5\n\n\u201c\u8d1f\u8d23\u63a5\u53e3\u6027\u80fd\u4f18\u5316\uff0c\u63d0\u5347\u4e86\u7cfb\u7edf\u541e\u5410\u80fd\u529b\u3002\u201d\n\n\u8fd9\u53e5\u8bdd\u6211\u81ea\u5df1\u770b\u7740\u90fd\u6ca1\u611f\u89c9\uff0c\u66f4\u522b\u8bf4\u9762\u8bd5\u5b98\u4e86\u3002\n\n## \u6211\u73b0\u5728\u7684\u5199\u6cd5\n\n\u6211\u628a\u5b83\u6539\u6210\uff1a\u201c\u9488\u5bf9\u9996\u9875\u63a8\u8350\u63a5\u53e3\u7684\u70ed\u70b9\u67e5\u8be2\u94fe\u8def\u8fdb\u884c\u4f18\u5316\uff0c\u901a\u8fc7 Redis \u7f13\u5b58 + \u6279\u91cf\u67e5\u8be2\u91cd\u6784\uff0c\u5c06 P95 \u54cd\u5e94\u65f6\u95f4\u4ece 320ms \u964d\u5230 140ms\u3002\u201d\n\n\u6539\u5b8c\u4e4b\u540e\u6211\u81ea\u5df1\u90fd\u89c9\u5f97\u5b9e\u5728\u591a\u4e86\uff0c\u56e0\u4e3a\u5b83\u8d77\u7801\u8bf4\u6e05\u695a\u4e86\uff1a\n\n- \u4f18\u5316\u7684\u662f\u4ec0\u4e48\u63a5\u53e3\n- \u4e3a\u4ec0\u4e48\u4f1a\u6162\n- \u7528\u4e86\u4ec0\u4e48\u65b9\u6cd5\n- \u6700\u540e\u6307\u6807\u6539\u5584\u4e86\u591a\u5c11\n\n\u611f\u89c9\u7b80\u5386\u771f\u7684\u5c31\u662f\u8981\u5f53\u6210\u4e00\u4efd\u201c\u53ef\u4ee5\u88ab\u8ffd\u95ee\u7684\u7b54\u6848\u201d\u53bb\u5199\uff0c\u4e0d\u7136\u5f88\u5bb9\u6613\u5199\u6210\u5927\u767d\u8bdd\u3002"
            ),
            new NoteSeed(
                    "seed_late_happy",
                    "HR \u9762\u6ca1\u6709\u60f3\u8c61\u4e2d\u90a3\u4e48\u7384\uff0c\u771f\u8bda\u53cd\u800c\u6bd4\u6807\u51c6\u7b54\u6848\u6709\u7528",
                    "\u6c42\u804c\u8bb0\u5f55",
                    "\ud83c\udf19",
                    79,
                    4,
                    "\u6211\u4e4b\u524d\u603b\u89c9\u5f97 HR \u9762\u662f\u201c\u80cc\u8bdd\u672f\u201d\uff0c\u4eca\u5929\u624d\u53d1\u73b0\u8bf4\u6e05\u695a\u81ea\u5df1\u7684\u771f\u5b9e\u60f3\u6cd5\u5176\u5b9e\u66f4\u91cd\u8981\u3002",
                    "\u4eca\u5929 HR \u9762\u95ee\u4e86\u6211 3 \u4e2a\u9898\uff0c\u5370\u8c61\u7279\u522b\u6df1\uff1a\n\n1. \u4f60\u4e3a\u4ec0\u4e48\u60f3\u505a\u540e\u7aef\uff1f\n2. \u4f60\u6700\u8fd1\u4e00\u6b21\u906d\u9047\u632b\u8d25\u662f\u4ec0\u4e48\uff1f\n3. \u5982\u679c offer \u6ca1\u4e0b\u6765\uff0c\u4f60\u4f1a\u600e\u4e48\u8c03\u6574\uff1f\n\n\u6211\u672c\u6765\u8fd8\u60f3\u6309\u7167\u6a21\u677f\u7b54\uff0c\u4f46\u540e\u6765\u5e72\u8106\u5c31\u8bf4\u5b9e\u8bdd\u4e86\uff1a\u6211\u5c31\u662f\u559c\u6b22\u90a3\u79cd\u628a\u4e00\u4e2a\u95ee\u9898\u62c6\u5f00\uff0c\u4ece\u63a5\u53e3\u5230\u5e93\u8868\u5230\u76d1\u63a7\u4e00\u5c42\u5c42\u6362\u6389\u7684\u611f\u89c9\uff1b\u524d\u9762\u6709\u51e0\u573a\u9762\u8bd5\u88ab\u95ee\u61f5\u4e86\uff0c\u6211\u6709\u70b9\u7740\u6025\uff0c\u4f46\u8fd9\u4e2a\u6708\u5f00\u59cb\u5b66\u4f1a\u6bcf\u573a\u53ea\u6293 2 \u4e2a\u771f\u95ee\u9898\u53bb\u8865\u3002\n\n\u7b54\u5b8c\u4e4b\u540e\u5bf9\u9762\u53cd\u800c\u8ddf\u6211\u804a\u5f97\u633a\u5bbd\u677e\u7684\u3002\u611f\u89c9 HR \u9762\u4e5f\u4e0d\u662f\u975e\u8981\u201c\u6ee1\u5206\u7b54\u6848\u201d\uff0c\u800c\u662f\u8981\u770b\u4f60\u662f\u4e0d\u662f\u77e5\u9053\u81ea\u5df1\u5728\u5e72\u561b\uff0c\u9047\u5230\u4e0d\u987a\u7684\u65f6\u5019\u4f1a\u4e0d\u4f1a\u8c03\u6574\u3002"
            ),
            new NoteSeed(
                    "seed_happy_loading",
                    "Redis \u6301\u4e45\u5316\u6211\u603b\u8bb0\u6df7\uff0c\u4eca\u5929\u7ec8\u4e8e\u7528\u4e00\u4e2a\u6545\u969c\u4f8b\u5b50\u628a\u5b83\u987a\u4e0b\u6765\u4e86",
                    "\u516b\u80a1\u6574\u7406",
                    "\ud83d\udee0",
                    72,
                    3,
                    "\u53ea\u80cc RDB \u548c AOF \u7684\u6982\u5ff5\u771f\u7684\u4e0d\u591f\uff0c\u628a\u201c\u5982\u679c\u7a81\u7136\u6302\u4e86\u4f1a\u4e22\u591a\u5c11\u6570\u636e\u201d\u60f3\u660e\u767d\u4e4b\u540e\uff0c\u8bb0\u5fc6\u70b9\u4e00\u4e0b\u5c31\u7a33\u4e86\u3002",
                    "## \u6211\u4e4b\u524d\u7684\u56f0\u60d1\n\n\u9762\u8bd5\u4e00\u95ee Redis \u6301\u4e45\u5316\uff0c\u6211\u5c31\u4f1a\u8bf4 RDB \u662f\u5feb\u7167\uff0cAOF \u662f\u547d\u4ee4\u8ffd\u52a0\uff0c\u7136\u540e\u5c31\u8bf4\u4e0d\u4e0b\u53bb\u4e86\u3002\u4f46\u4e00\u65e6\u95ee\u5230\u201c\u90a3\u4f60\u7ebf\u4e0a\u4f1a\u600e\u4e48\u9009\uff1f\u201d\uff0c\u6211\u5c31\u8fd8\u662f\u865a\u3002\n\n## \u540e\u6765\u6211\u7528\u4e00\u4e2a\u6545\u969c\u573a\u666f\u53bb\u8bb0\n\n\u6211\u7ed9\u81ea\u5df1\u5199\u4e86\u4e00\u4e2a\u975e\u5e38\u7b28\u7684\u573a\u666f\uff1a\u5982\u679c\u4eca\u5929 Redis \u91cc\u653e\u7684\u662f\u4e0b\u5355\u540e 5 \u5206\u949f\u5185\u8981\u7528\u5230\u7684\u72b6\u6001\u6570\u636e\uff0c\u673a\u5668\u7a81\u7136\u6302\u4e86\uff0c\u4f60\u80fd\u63a5\u53d7\u4e22\u591a\u5c11\uff1f\n\n- \u5982\u679c\u53ef\u4ee5\u63a5\u53d7\u4e22\u6700\u8fd1\u4e00\u5c0f\u6bb5\uff0cRDB \u53ef\u80fd\u5c31\u591f\u7528\n- \u5982\u679c\u5199\u5165\u4e0d\u60f3\u4e22\uff0c\u5c31\u8981\u8003\u8651 AOF everysec \u751a\u81f3\u66f4\u4e25\u683c\u7684\u7b56\u7565\n- \u5982\u679c\u8fd8\u6709\u4e3b\u4ece\u5207\u6362\uff0c\u5c31\u8981\u518d\u8bf4\u6e05\u695a\u6570\u636e\u4e00\u81f4\u6027\u9884\u671f\n\n\u611f\u89c9\u8fd9\u79cd\u8bb0\u6cd5\u5bf9\u6211\u633a\u6709\u7528\u7684\uff0c\u56e0\u4e3a\u6211\u4e00\u65e6\u80fd\u628a\u573a\u666f\u8bf4\u987a\uff0c\u540e\u9762\u7684\u4f18\u7f3a\u70b9\u5c31\u4e0d\u662f\u6b7b\u80cc\u4e86\u3002"
            ),
            new NoteSeed(
                    "seed_human_observer",
                    "\u770b\u4e8610 \u591a\u7bc7\u725b\u5ba2\u9762\u7ecf\u540e\uff0c\u6211\u628a\u9ad8\u9891\u8ffd\u95ee\u6574\u7406\u6210\u4e86\u4e00\u5f20\u8868",
                    "\u9762\u7ecf\u6574\u7406",
                    "\ud83d\udce1",
                    65,
                    2,
                    "\u5149\u770b\u9898\u76ee\u6ca1\u7528\uff0c\u6211\u540e\u6765\u53d1\u73b0\u771f\u6b63\u6709\u7528\u7684\u662f\u201c\u8fd9\u9898\u95ee\u5b8c\u4e4b\u540e\u8fd8\u4f1a\u63a5\u7740\u95ee\u4ec0\u4e48\u201d\u3002",
                    "\u8fd9\u4e24\u5929\u5237\u4e86\u4e00\u5806\u725b\u5ba2\u548c\u535a\u5ba2\u91cc\u7684 Java \u540e\u7aef\u9762\u7ecf\uff0c\u611f\u89c9\u6700\u5e38\u51fa\u73b0\u7684\u4e0d\u662f\u5355\u72ec\u7684\u4e00\u9053\u9898\uff0c\u800c\u662f\u4e00\u6574\u4e32\u8ffd\u95ee\u3002\n\n\u6211\u73b0\u5728\u6574\u7406\u4e86\u4e00\u5f20\u8868\uff0c\u5de6\u8fb9\u662f\u9ad8\u9891\u4e3b\u9898\uff0c\u53f3\u8fb9\u662f\u5e38\u89c1\u8ffd\u95ee\uff1a\n\n- Redis -> \u4e3a\u4ec0\u4e48\u7528\uff0ckey \u600e\u4e48\u8bbe\u8ba1\uff0c\u7f13\u5b58\u4e0e\u6570\u636e\u5e93\u600e\u4e48\u4fdd\u6301\u4e00\u81f4\n- MySQL -> \u7d22\u5f15\u4e3a\u4ec0\u4e48\u751f\u6548/\u5931\u6548\uff0cexplain \u600e\u4e48\u770b\uff0c\u6162 SQL \u600e\u4e48\u4f18\u5316\n- Spring \u4e8b\u52a1 -> \u4e3a\u4ec0\u4e48\u5931\u6548\uff0c\u81ea\u8c03\u7528\u95ee\u9898\uff0c\u5f02\u5e38\u56de\u6eda\u89c4\u5219\n- \u7ebf\u7a0b\u6c60 -> \u53c2\u6570\u600e\u4e48\u914d\uff0c\u8d85\u8fc7\u6838\u5fc3\u7ebf\u7a0b\u6570\u540e\u4f1a\u53d1\u751f\u4ec0\u4e48\n- \u9879\u76ee -> \u6700\u5927\u6d41\u91cf\uff0c\u6700\u96be\u7684 bug\uff0c\u5982\u679c\u518d\u8bbe\u8ba1\u4e00\u904d\u4f1a\u6539\u54ea\u91cc\n\n\u6211\u53d1\u73b0\u81ea\u5df1\u4ee5\u524d\u603b\u5728\u8865\u201c\u70b9\u201d\uff0c\u4f46\u9762\u8bd5\u5b98\u66f4\u7231\u542c\u201c\u94fe\u8def\u201d\u3002\u73b0\u5728\u770b\u9762\u7ecf\u4e5f\u6ca1\u90a3\u4e48\u7126\u8651\u4e86\uff0c\u56e0\u4e3a\u6211\u4f1a\u8fb9\u770b\u8fb9\u95ee\u81ea\u5df1\uff1a\u5982\u679c\u8fd9\u9898\u95ee\u5230\u6211\uff0c\u6211\u540e\u9762\u4f1a\u88ab\u8ffd\u95ee\u5230\u54ea\u4e00\u5c42\uff1f"
            ),
            new NoteSeed(
                    "seed_silent_mode",
                    "\u63a5\u53e3\u6587\u6863\u8865\u5b8c\u4e4b\u540e\uff0c\u6211\u624d\u6562\u8bf4\u81ea\u5df1\u771f\u7684\u505a\u8fc7\u8fd9\u4e2a\u9879\u76ee",
                    "\u9879\u76ee\u590d\u76d8",
                    "\ud83d\uddd2\ufe0f",
                    59,
                    1,
                    "\u6700\u8fd1\u6ca1\u6709\u72c2\u5237\u9898\uff0c\u53cd\u800c\u5728\u8865\u9879\u76ee\u6587\u6863\u3002\u7ed3\u679c\u53d1\u73b0\u8fd9\u4ef6\u4e8b\u5bf9\u9762\u8bd5\u7684\u5e2e\u52a9\u6bd4\u6211\u60f3\u8c61\u4e2d\u5927\u5f97\u591a\u3002",
                    "\u6211\u8fd9\u4e24\u5929\u628a\u505a\u8fc7\u7684\u4e00\u4e2a\u5c0f\u9879\u76ee\u91cd\u65b0\u8865\u4e86\u4e00\u4efd\u6587\u6863\uff0c\u91cc\u9762\u53ea\u5199 4 \u4ef6\u4e8b\uff1a\n\n- \u63a5\u53e3\u5165\u53c2\u51fa\u53c2\u662f\u4ec0\u4e48\n- \u5e93\u8868\u6620\u5c04\u548c\u6838\u5fc3\u6d41\u8f6c\u662f\u600e\u4e48\u8d70\u7684\n- \u54ea\u4e9b\u5730\u65b9\u6709\u8fb9\u754c\u60c5\u51b5\n- \u7ebf\u4e0a\u51fa\u95ee\u9898\u65f6\u6700\u53ef\u80fd\u67e5\u54ea\u51e0\u4e2a\u70b9\n\n\u8865\u5b8c\u4e4b\u540e\u6211\u518d\u56de\u5934\u770b\u7b80\u5386\uff0c\u7a81\u7136\u89c9\u5f97\u81ea\u5df1\u4e4b\u524d\u5bf9\u9879\u76ee\u7684\u7406\u89e3\u5176\u5b9e\u633a\u6d6e\u7684\u3002\u5f88\u591a\u65f6\u5019\u6211\u4ee5\u4e3a\u81ea\u5df1\u4f1a\u8bb2\uff0c\u53ea\u662f\u56e0\u4e3a\u6211\u8bb0\u5f97\u529f\u80fd\uff0c\u4f46\u771f\u95ee\u5230\u5f02\u5e38\u5206\u652f\u3001\u5e42\u7b49\u3001\u63a5\u53e3\u8fb9\u754c\uff0c\u8fd8\u662f\u5f97\u60f3\u534a\u5929\u3002\n\n\u6240\u4ee5\u6211\u73b0\u5728\u7684\u60f3\u6cd5\u662f\uff0c\u9879\u76ee\u6587\u6863\u4e0d\u53ea\u662f\u4e3a\u4e86\u5de5\u4f5c\u7559\u6863\uff0c\u5bf9\u6c42\u804c\u4e5f\u5f88\u6709\u7528\u3002\u81f3\u5c11\u5b83\u80fd\u5e2e\u6211\u5206\u6e05\u695a\uff1a\u54ea\u4e9b\u662f\u6211\u771f\u4f1a\u7684\uff0c\u54ea\u4e9b\u53ea\u662f\u6211\u4ee5\u4e3a\u6211\u4f1a\u3002"
            )
    );

    private static final List<WishSeed> WISH_SEEDS = List.of(
            new WishSeed("\u5feb\u4e50\u4e0d\u5728\u670d\u52a1\u533a\uff0c\u4f46\u6211\u60f3\u628a\u751f\u6d3b\u91cd\u65b0\u8fde\u4e0a\u4fe1\u53f7\u3002", "hopeful", "#D7E8D4", "\u676d\u5dde", 14, 18, 1.05, 9),
            new WishSeed("\u5e0c\u671b\u6700\u8fd1\u6295\u51fa\u7684\u5c97\u4f4d\u91cc\uff0c\u80fd\u6536\u5230\u4e00\u5c01\u8ba4\u771f\u4e00\u70b9\u7684\u56de\u590d\u3002", "hopeful", "#C9D8E8", "\u4e0a\u6d77", 53, 28, 0.96, 8),
            new WishSeed("\u60f3\u628a\u90a3\u5957\u4e00\u76f4\u6ca1\u8bf4\u987a\u7684\u9879\u76ee\u7ecf\u5386\uff0c\u7ec8\u4e8e\u8bb2\u5230\u81ea\u5df1\u4e5f\u6ee1\u610f\u3002", "confused", "#D6D3E8", "\u5357\u4eac", 27, 62, 1.12, 7),
            new WishSeed("\u4f4e\u6c14\u538b\u7684\u65f6\u5019\uff0c\u4e5f\u60f3\u7a33\u7a33\u628a\u4eca\u5929\u8fc7\u5b8c\u3002", "anxious", "#E6D7C8", "\u6b66\u6c49", 68, 73, 1.28, 6),
            new WishSeed("\u5e0c\u671b\u4e0b\u4e00\u6b21\u590d\u76d8\u7684\u65f6\u5019\uff0c\u6211\u4f1a\u56e0\u4e3a\u575a\u6301\u800c\u4e0d\u662f\u56e0\u4e3a\u61ca\u6094\u5199\u5b57\u3002", "hopeful", "#E7E0CF", "\u6210\u90fd", 76, 34, 0.92, 5),
            new WishSeed("\u60f3\u628a\u665a\u70b9\u5f00\u5fc3\u771f\u7684\u7b49\u5230\uff0c\u800c\u4e0d\u662f\u534a\u8def\u5148\u653e\u5f03\u3002", "hopeful", "#E5CFE0", "\u53a6\u95e8", 44, 52, 1.08, 4),
            new WishSeed("\u7ed9\u81ea\u5df1\u6512\u4e00\u70b9\u770b\u5f97\u89c1\u7684\u8fdb\u5ea6\uff0c\u522b\u518d\u603b\u89c9\u5f97\u539f\u5730\u8e0f\u6b65\u3002", "determined", "#D7E4C6", "\u6df1\u5733", 82, 21, 0.88, 3),
            new WishSeed("\u613f\u6211\u8fd8\u80fd\u4fdd\u6301\u89c2\u5bdf\u4e16\u754c\u7684\u8010\u5fc3\uff0c\u4e5f\u4fdd\u6301\u4e00\u70b9\u6e29\u67d4\u3002", "happy", "#F0E2C6", "\u82cf\u5dde", 33, 41, 1.00, 2),
            new WishSeed("\u6c89\u9ed8\u7684\u65f6\u5019\uff0c\u4e5f\u80fd\u88ab\u7406\u89e3\uff0c\u4e0d\u7528\u603b\u9760\u70ed\u95f9\u8bc1\u660e\u5b58\u5728\u3002", "confused", "#CFD8E2", "\u5317\u4eac", 59, 14, 1.14, 1)
    );

    private static final List<String> NOTE_COMMENTS = List.of(
            "\u6211\u4e5f\u662f\u6302\u8fc7\u4e00\u6b21\u9879\u76ee\u6df1\u6326\u624d\u53cd\u5e94\u8fc7\u6765\uff0c\u9762\u8bd5\u5b98\u771f\u7684\u5c31\u7231\u95ee\u201c\u7ebf\u4e0a\u771f\u5b9e\u60c5\u51b5\u201d\u3002",
            "\u7d22\u5f15\u8fd9\u5757\u6211\u4e5f\u662f explain \u770b\u5f97\u5c11\uff0c\u6bcf\u6b21\u4e00\u88ab\u95ee SQL \u5c31\u865a\u3002",
            "\u201c\u77e5\u8bc6\u70b9 + \u9879\u76ee\u573a\u666f\u201d\u8fd9\u79cd\u8bb0\u6cd5\u6211\u51c6\u5907\u4eca\u665a\u5c31\u8bd5\u4e00\u4e0b\u3002",
            "\u6211\u4e4b\u524d\u4e5f\u662f\u4e71\u6295\uff0c\u540e\u6765\u53d1\u73b0 JD \u4e0d\u5bf9\u7740\u6539\u7b80\u5386\u771f\u7684\u767d\u6295\u3002",
            "\u7b80\u5386\u91cc\u6709\u6570\u5b57\u548c\u524d\u540e\u5bf9\u6bd4\u4e4b\u540e\uff0c\u9762\u8bd5\u5b98\u8ffd\u95ee\u90fd\u4f1a\u66f4\u987a\u4e00\u70b9\u3002",
            "HR \u9762\u8fd9\u5757\u6211\u4e5f\u662f\u540e\u6765\u624d\u53d1\u73b0\uff0c\u6b7b\u80cc\u6a21\u677f\u771f\u7684\u4e0d\u5982\u8bf4\u4eba\u8bdd\u3002",
            "\u7528\u6545\u969c\u573a\u666f\u8bb0 Redis \u8fd9\u4e2a\u611f\u89c9\u633a\u597d\uff0c\u6211\u8001\u662f\u628a RDB \u548c AOF \u80cc\u4e32\u4e86\u3002",
            "\u725b\u5ba2\u9762\u7ecf\u771f\u7684\u8981\u770b\u8ffd\u95ee\uff0c\u4e0d\u7136\u770b\u5b8c\u4e00\u5806\u9898\u76ee\u8fd8\u662f\u4e0d\u4f1a\u7b54\u3002",
            "\u8865\u6587\u6863\u8fd9\u4e2a\u6211\u592a\u6709\u5171\u9e23\u4e86\uff0c\u5199\u5b8c\u624d\u53d1\u73b0\u81ea\u5df1\u539f\u6765\u6709\u90a3\u4e48\u591a\u7ec6\u8282\u6ca1\u60f3\u6e05\u695a\u3002"
    );

    private static final List<String> WISH_COMMENTS = List.of(
            "\u613f\u4f60\u8fd9\u6b21\u771f\u7684\u6536\u5230\u60f3\u8981\u7684\u56de\u97f3\u3002",
            "\u5148\u628a\u4eca\u5929\u8fc7\u597d\uff0c\u540e\u9762\u7684\u597d\u6d88\u606f\u4f1a\u6162\u6162\u9760\u8fd1\u3002",
            "\u6709\u4e9b\u613f\u671b\u4e0d\u662f\u665a\u5230\uff0c\u662f\u5728\u8def\u4e0a\u3002"
    );

    private static final Map<String, RankSeed> RANK_SEEDS = Map.of(
            "seed_joy_offline", new RankSeed(8, 3, 1260),
            "seed_cloudy_today", new RankSeed(7, 2, 1185),
            "seed_too_quiet", new RankSeed(6, 2, 1098),
            "seed_low_pressure", new RankSeed(6, 1, 988),
            "seed_mood_overcast", new RankSeed(5, 2, 932),
            "seed_late_happy", new RankSeed(4, 1, 886),
            "seed_happy_loading", new RankSeed(4, 3, 841),
            "seed_human_observer", new RankSeed(3, 2, 768),
            "seed_silent_mode", new RankSeed(3, 1, 702)
    );

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final UserQuestionMasteryMapper masteryMapper;
    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final WishMapper wishMapper;
    private final WishLikeMapper wishLikeMapper;
    private final WishCommentMapper wishCommentMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public CommunitySeedDataInitializer(
            UserMapper userMapper,
            QuestionMapper questionMapper,
            UserQuestionMasteryMapper masteryMapper,
            NoteMapper noteMapper,
            NoteLikeMapper noteLikeMapper,
            NoteCommentMapper noteCommentMapper,
            WishMapper wishMapper,
            WishLikeMapper wishLikeMapper,
            WishCommentMapper wishCommentMapper,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redisTemplate,
            JdbcTemplate jdbcTemplate
    ) {
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
        this.masteryMapper = masteryMapper;
        this.noteMapper = noteMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.noteCommentMapper = noteCommentMapper;
        this.wishMapper = wishMapper;
        this.wishLikeMapper = wishLikeMapper;
        this.wishCommentMapper = wishCommentMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Map<String, User> users = ensureUsers();
            ensureAvatars(users);
            List<Long> questionIds = loadQuestionIds();
            ensureRankData(users, questionIds);
            Map<String, Note> notes = ensureNotes(users);
            ensureNoteInteractions(users, notes);
            Map<String, Wish> wishes = ensureWishes();
            ensureWishInteractions(users, wishes);
            ensureGameLogs(users);
            log.info("community seed ready, users={}", users.size());
        } catch (Exception e) {
            log.warn("community seed skipped: {}", e.getMessage(), e);
        }
    }

    private void ensureAvatars(Map<String, User> users) {
        for (SeedUser seed : SEED_USERS) {
            if (!users.containsKey(seed.username())) {
                continue;
            }
            redisTemplate.opsForValue().set(PROFILE_AVATAR_KEY_PREFIX + seed.username(), seed.avatar());
        }
    }

    private Map<String, User> ensureUsers() {
        Map<String, User> existing = loadUsers();
        for (SeedUser seed : SEED_USERS) {
            if (existing.containsKey(seed.username())) {
                continue;
            }
            User user = new User();
            user.setUsername(seed.username());
            user.setPassword(passwordEncoder.encode("seed-" + seed.username() + "-" + System.nanoTime()));
            user.setNickname(seed.nickname());
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now().minusDays(seed.daysAgo()));
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException ignore) {
            }
        }
        return loadUsers();
    }

    private Map<String, User> loadUsers() {
        List<String> usernames = SEED_USERS.stream().map(SeedUser::username).toList();
        List<User> rows = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUsername, usernames));
        Map<String, User> result = new LinkedHashMap<>();
        for (User row : rows) {
            result.put(row.getUsername(), row);
        }
        return result;
    }

    private List<Long> loadQuestionIds() {
        List<Question> rows = questionMapper.selectList(
                new LambdaQueryWrapper<Question>().select(Question::getId).eq(Question::getStatus, 1).orderByAsc(Question::getId).last("LIMIT 120")
        );
        List<Long> result = new ArrayList<>();
        for (Question row : rows) {
            if (row.getId() != null) {
                result.add(row.getId());
            }
        }
        return result;
    }

    private void ensureRankData(Map<String, User> users, List<Long> questionIds) {
        int cursor = 0;
        for (SeedUser seedUser : SEED_USERS) {
            User user = users.get(seedUser.username());
            RankSeed rankSeed = RANK_SEEDS.get(seedUser.username());
            if (user == null || user.getId() == null || rankSeed == null) {
                continue;
            }

            Set<Long> existing = loadMastered(user.getId());
            int limit = Math.min(rankSeed.questionDone(), questionIds.size());
            for (int i = 0; i < limit; i++) {
                Long questionId = questionIds.get((cursor + i) % questionIds.size());
                if (existing.contains(questionId)) {
                    continue;
                }
                UserQuestionMastery mastery = new UserQuestionMastery();
                LocalDateTime time = LocalDateTime.now().minusDays(seedUser.daysAgo()).plusHours(i + 1L);
                mastery.setUserId(user.getId());
                mastery.setQuestionId(questionId);
                mastery.setConfirmedAt(time);
                mastery.setCreatedAt(time);
                mastery.setUpdatedAt(time);
                try {
                    masteryMapper.insert(mastery);
                } catch (DuplicateKeyException ignore) {
                }
            }
            cursor += 4;

            Set<Long> finalIds = loadMastered(user.getId());
            if (!finalIds.isEmpty()) {
                redisTemplate.opsForSet().add(Q_SET_PREFIX + seedUser.username(), finalIds.stream().map(String::valueOf).toArray(String[]::new));
            }
            redisTemplate.opsForZSet().add(Q_COUNT_KEY, seedUser.username(), finalIds.size());

            Double wishCount = redisTemplate.opsForZSet().score(WISH_COUNT_KEY, seedUser.username());
            if (wishCount == null || Math.round(wishCount) < rankSeed.wishCount()) {
                redisTemplate.opsForZSet().add(WISH_COUNT_KEY, seedUser.username(), rankSeed.wishCount());
            }
            Double gameBest = redisTemplate.opsForZSet().score(GAME_BEST_KEY, seedUser.username());
            if (gameBest == null || Math.round(gameBest) < rankSeed.gameBest()) {
                redisTemplate.opsForZSet().add(GAME_BEST_KEY, seedUser.username(), rankSeed.gameBest());
            }
        }
    }

    private Set<Long> loadMastered(Long userId) {
        List<UserQuestionMastery> rows = masteryMapper.selectList(new LambdaQueryWrapper<UserQuestionMastery>().eq(UserQuestionMastery::getUserId, userId));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (UserQuestionMastery row : rows) {
            if (row.getQuestionId() != null) {
                result.add(row.getQuestionId());
            }
        }
        return result;
    }

    private Map<String, Note> ensureNotes(Map<String, User> users) {
        Map<String, Note> result = new LinkedHashMap<>();
        for (NoteSeed seed : NOTE_SEEDS) {
            User owner = users.get(seed.ownerUsername());
            if (owner == null || owner.getId() == null) {
                continue;
            }
            Note existing = noteMapper.selectOne(
                    new LambdaQueryWrapper<Note>()
                            .eq(Note::getUserId, owner.getId())
                            .eq(Note::getAuditSource, "SYSTEM_SEED")
                            .orderByDesc(Note::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (existing == null) {
                Note note = new Note();
                LocalDateTime time = LocalDateTime.now().minusDays(seed.daysAgo()).withHour(21).withMinute(8);
                note.setUserId(owner.getId());
                note.setAuthor(owner.getNickname());
                note.setTitle(seed.title());
                note.setSummary(seed.summary());
                note.setContent(seed.content());
                note.setCategory(seed.category());
                note.setEmoji(seed.emoji());
                note.setViewCount(seed.views());
                note.setStatus(1);
                note.setAuditSource("SYSTEM_SEED");
                note.setAuditedAt(time.plusMinutes(5));
                note.setCreatedAt(time);
                note.setUpdatedAt(time.plusMinutes(10));
                note.setDeleted(0);
                noteMapper.insert(note);
                existing = note;
            } else {
                LocalDateTime time = LocalDateTime.now().minusDays(seed.daysAgo()).withHour(21).withMinute(8);
                existing.setAuthor(owner.getNickname());
                existing.setTitle(seed.title());
                existing.setSummary(seed.summary());
                existing.setContent(seed.content());
                existing.setCategory(seed.category());
                existing.setEmoji(seed.emoji());
                existing.setViewCount(seed.views());
                existing.setStatus(1);
                existing.setAuditSource("SYSTEM_SEED");
                existing.setAuditedAt(time.plusMinutes(5));
                existing.setCreatedAt(time);
                existing.setUpdatedAt(time.plusMinutes(10));
                existing.setDeleted(0);
                noteMapper.updateById(existing);
            }
            result.put(seed.ownerUsername(), existing);
        }
        return result;
    }

    private void ensureNoteInteractions(Map<String, User> users, Map<String, Note> notes) {
        for (int i = 0; i < SEED_USERS.size(); i++) {
            SeedUser ownerSeed = SEED_USERS.get(i);
            Note note = notes.get(ownerSeed.username());
            if (note == null || note.getId() == null) {
                continue;
            }

            SeedUser likeSeed = SEED_USERS.get((i + 1) % SEED_USERS.size());
            if (noteLikeMapper.selectOne(new LambdaQueryWrapper<NoteLike>()
                    .eq(NoteLike::getNoteId, note.getId())
                    .eq(NoteLike::getActorType, "user")
                    .eq(NoteLike::getActorId, likeSeed.username())
                    .last("LIMIT 1")) == null) {
                NoteLike like = new NoteLike();
                like.setNoteId(note.getId());
                like.setActorType("user");
                like.setActorId(likeSeed.username());
                like.setCreatedAt(LocalDateTime.now().minusDays(Math.max(1, ownerSeed.daysAgo() - 1)));
                noteLikeMapper.insert(like);
            }

            SeedUser commentSeed = SEED_USERS.get((i + SEED_USERS.size() - 1) % SEED_USERS.size());
            User commentUser = users.get(commentSeed.username());
            String content = NOTE_COMMENTS.get(i % NOTE_COMMENTS.size());
            if (commentUser != null && commentUser.getId() != null) {
                NoteComment existingComment = noteCommentMapper.selectOne(new LambdaQueryWrapper<NoteComment>()
                        .eq(NoteComment::getNoteId, note.getId())
                        .eq(NoteComment::getUserId, commentUser.getId())
                        .last("LIMIT 1"));
                if (existingComment == null) {
                    NoteComment comment = new NoteComment();
                    comment.setNoteId(note.getId());
                    comment.setUserId(commentUser.getId());
                    comment.setUsername(commentUser.getUsername());
                    comment.setNickname(commentUser.getNickname());
                    comment.setContent(content);
                    comment.setCreatedAt(LocalDateTime.now().minusDays(Math.max(1, ownerSeed.daysAgo() - 1)).plusHours(2));
                    comment.setDeleted(0);
                    noteCommentMapper.insert(comment);
                } else {
                    existingComment.setUsername(commentUser.getUsername());
                    existingComment.setNickname(commentUser.getNickname());
                    existingComment.setContent(content);
                    existingComment.setDeleted(0);
                    noteCommentMapper.updateById(existingComment);
                }
            }
        }
    }

    private Map<String, Wish> ensureWishes() {
        Map<String, Wish> result = new LinkedHashMap<>();
        for (WishSeed seed : WISH_SEEDS) {
            Wish existing = wishMapper.selectOne(new LambdaQueryWrapper<Wish>().eq(Wish::getContent, seed.content()).last("LIMIT 1"));
            if (existing == null) {
                Wish wish = new Wish();
                wish.setContent(seed.content());
                wish.setEmotion(seed.emotion());
                wish.setColor(seed.color());
                wish.setCity(seed.city());
                wish.setPosX(seed.posX());
                wish.setPosY(seed.posY());
                wish.setFloatSpeed(seed.speed());
                wish.setStatus(1);
                wish.setCreatedAt(LocalDateTime.now().minusDays(seed.daysAgo()).withHour(22).withMinute(36));
                wishMapper.insert(wish);
                existing = wish;
            }
            result.put(seed.content(), existing);
        }
        return result;
    }

    private void ensureWishInteractions(Map<String, User> users, Map<String, Wish> wishes) {
        for (int i = 0; i < WISH_SEEDS.size(); i++) {
            WishSeed seed = WISH_SEEDS.get(i);
            Wish wish = wishes.get(seed.content());
            if (wish == null || wish.getId() == null) {
                continue;
            }

            SeedUser likeSeed = SEED_USERS.get((i + 2) % SEED_USERS.size());
            if (wishLikeMapper.selectOne(new LambdaQueryWrapper<WishLike>()
                    .eq(WishLike::getWishId, wish.getId())
                    .eq(WishLike::getActorType, "user")
                    .eq(WishLike::getActorId, likeSeed.username())
                    .last("LIMIT 1")) == null) {
                WishLike like = new WishLike();
                like.setWishId(wish.getId());
                like.setActorType("user");
                like.setActorId(likeSeed.username());
                like.setCreatedAt(LocalDateTime.now().minusDays(Math.max(1, seed.daysAgo() - 1)));
                wishLikeMapper.insert(like);
            }

            SeedUser commentSeed = SEED_USERS.get((i + 1) % SEED_USERS.size());
            User commentUser = users.get(commentSeed.username());
            String content = WISH_COMMENTS.get(i % WISH_COMMENTS.size());
            if (commentUser != null && commentUser.getId() != null && wishCommentMapper.selectOne(new LambdaQueryWrapper<WishComment>()
                    .eq(WishComment::getWishId, wish.getId())
                    .eq(WishComment::getUserId, commentUser.getId())
                    .eq(WishComment::getContent, content)
                    .last("LIMIT 1")) == null) {
                WishComment comment = new WishComment();
                comment.setWishId(wish.getId());
                comment.setUserId(commentUser.getId());
                comment.setUsername(commentUser.getUsername());
                comment.setNickname(commentUser.getNickname());
                comment.setContent(content);
                comment.setCreatedAt(LocalDateTime.now().minusDays(Math.max(1, seed.daysAgo() - 1)).plusHours(3));
                comment.setDeleted(0);
                wishCommentMapper.insert(comment);
            }
        }
    }

    private void ensureGameLogs(Map<String, User> users) {
        for (SeedUser seed : SEED_USERS) {
            User user = users.get(seed.username());
            RankSeed rank = RANK_SEEDS.get(seed.username());
            if (user == null || user.getId() == null || rank == null) {
                continue;
            }
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM game_score_logs WHERE user_id = ?", Integer.class, user.getId());
            if (count != null && count > 0) {
                continue;
            }
            int first = Math.max(120, rank.gameBest() - 420);
            int second = Math.max(240, rank.gameBest() - 170);
            LocalDateTime base = LocalDateTime.now().minusDays(seed.daysAgo()).withHour(20).withMinute(40);
            String roundId = "seed-round-" + seed.username();
            jdbcTemplate.update("INSERT INTO game_score_logs(user_id, username, round_id, score, is_final, recorded_at) VALUES (?, ?, ?, ?, ?, ?)",
                    user.getId(), user.getUsername(), roundId, first, 0, base);
            jdbcTemplate.update("INSERT INTO game_score_logs(user_id, username, round_id, score, is_final, recorded_at) VALUES (?, ?, ?, ?, ?, ?)",
                    user.getId(), user.getUsername(), roundId, second, 0, base.plusSeconds(8));
            jdbcTemplate.update("INSERT INTO game_score_logs(user_id, username, round_id, score, is_final, recorded_at) VALUES (?, ?, ?, ?, ?, ?)",
                    user.getId(), user.getUsername(), roundId, rank.gameBest(), 1, base.plusSeconds(16));
        }
    }

    private record SeedUser(String username, String nickname, String avatar, int daysAgo) {
    }

    private record NoteSeed(String ownerUsername, String title, String category, String emoji, int views, int daysAgo, String summary, String content) {
    }

    private record WishSeed(String content, String emotion, String color, String city, int posX, int posY, double speed, int daysAgo) {
    }

    private record RankSeed(int questionDone, int wishCount, int gameBest) {
    }
}
