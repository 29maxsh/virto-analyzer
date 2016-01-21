package ru.VirtaMarketAnalyzer.publish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.data.RetailAnalytics;
import ru.VirtaMarketAnalyzer.main.Utils;
import ru.VirtaMarketAnalyzer.main.Wizard;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cobr123 on 06.05.2015.
 */
final public class GitHubPublisher {
    private static final Logger logger = LoggerFactory.getLogger(GitHubPublisher.class);
    public static final String localPath = Utils.getDir() + "remote_repository" + File.separator;

    public static void publish(final List<String> realms) throws IOException, GitAPIException {
        final Git git = getRepo();
        copyToLocalRepo(realms);
        logger.info("git add .");
        git.add().addFilepattern(".").call();
        logger.info("git commit");
        git.commit().setMessage("data update").call();
        logger.info("git push");
        git.push().setCredentialsProvider(getCredentialsProvider()).call();
        git.close();
    }

    public static List<String> getAllVersions(final Git git, final String file) throws IOException, GitAPIException {
        final List<String> list = new ArrayList<>();
        final Iterable<RevCommit> logs = git.log()
                .addPath(file)
                .call();
        for (final RevCommit rev : logs) {
            logger.trace("Commit: {}, name: {}, id: {}", rev, rev.getName(), rev.getId().getName());
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            getFileFromCommit(os, file, git.getRepository(), rev.getTree());
            list.add(os.toString("UTF-8"));
        }
        return list;
    }

    public static void getFileFromCommit(final OutputStream os, final String file, final Repository repo, final RevTree tree) throws IOException, GitAPIException {
        final TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(file));
        if (!treeWalk.next()) {
            throw new IllegalStateException("Did not find expected file '" + file + "'");
        }

        final ObjectId objectId = treeWalk.getObjectId(0);
        final ObjectLoader loader = repo.open(objectId);

        // and then one can the loader to read the file
        loader.copyTo(os);
    }


    private static void copyToLocalRepo(final List<String> realms) throws IOException {
        for (final String realm : realms) {
            final File srcDir = new File(Utils.getDir() + Wizard.by_trade_at_cities + File.separator + realm + File.separator);
            if (srcDir.exists()) {
                final File destDir = new File(localPath + Wizard.by_trade_at_cities + File.separator + realm + File.separator);
                if (destDir.exists()) {
                    logger.info("удаляем {}", destDir.getAbsolutePath());
                    FileUtils.deleteDirectory(destDir);
                }
                logger.info("копируем {} в {}", srcDir.getAbsolutePath(), destDir.getAbsolutePath());
                FileUtils.copyDirectory(srcDir, destDir);
            }
        }
        for (final String realm : realms) {
            final File srcDir = new File(Utils.getDir() + Wizard.industry + File.separator + realm + File.separator);
            if (srcDir.exists()) {
                final File destDir = new File(localPath + Wizard.industry + File.separator + realm + File.separator);
                if (destDir.exists()) {
                    logger.info("удаляем {}", destDir.getAbsolutePath());
                    FileUtils.deleteDirectory(destDir);
                }
                logger.info("копируем {} в {}", srcDir.getAbsolutePath(), destDir.getAbsolutePath());
                FileUtils.copyDirectory(srcDir, destDir);
            }
        }
    }

    private static CredentialsProvider getCredentialsProvider() {
        final String token = System.getenv("vma.github.token");
        final String name = System.getenv("vma.github.username");
        if ((name == null || name.isEmpty()) && (token == null || token.isEmpty())) {
            throw new IllegalArgumentException("Необходимо задать логин к репозиторию (vma.github.username) или токен (vma.github.token)");
        }
        final String password = System.getenv("vma.github.password");
        if (name != null && !name.isEmpty() && (password == null || password.isEmpty())) {
            throw new IllegalArgumentException("Не задан пароль к репозиторию (vma.github.password)");
        }
        if (token != null && !token.isEmpty()) {
            logger.info("auth by token");
            return new UsernamePasswordCredentialsProvider(token, "");
        } else {
            logger.info("auth by username and password");
            return new UsernamePasswordCredentialsProvider(name, password);
        }
    }

    public static Git getRepo() throws IOException, GitAPIException {
        return getRepo(new File(localPath));
    }

    public static Git getRepo(final File localPathFile) throws IOException, GitAPIException {
        if (localPathFile.exists()) {
            logger.info("git open");
            final Git git = Git.open(localPathFile);
            logger.info("git pull");
            git.pull().call();
            return git;
        } else {
            //"https://github.com/user/repo.git"
            final String remotePath = System.getenv("vma.github.remotepath");
            if (remotePath == null || remotePath.isEmpty()) {
                throw new IllegalArgumentException("Не задан удаленный путь к репозиторию (vma.github.remotepath), например https://github.com/user/repo.git");
            }
            logger.info("git clone {} в {}", remotePath, localPathFile.getAbsolutePath());
            final CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(remotePath);
            cloneCommand.setDirectory(localPathFile);
            cloneCommand.setCredentialsProvider(getCredentialsProvider());
            return cloneCommand.call();
        }
    }

    public static void testCommit() throws IOException, GitAPIException {
        logger.info("localPath: " + localPath);
        final Git git = getRepo();
        logger.info("git commit");
        git.commit().setMessage("test").call();
        logger.info("git push");
        git.push().setCredentialsProvider(getCredentialsProvider()).call();
        git.close();
    }

    public static void testPull() throws IOException, GitAPIException {
        logger.info("localPath: " + localPath);
        final Git git = getRepo();
    }

    public static void testGetAllVersions() throws IOException, GitAPIException {
        final Git git = getRepo();
        final List<String> list = getAllVersions(git, Wizard.by_trade_at_cities + "/" + "olga" + "/" + "retail_analytics_1526.json");
        final Set<RetailAnalytics> set = new HashSet<>();

        for (String file : list) {
            try {
                logger.info(file);
                final RetailAnalytics[] arr = new GsonBuilder().create().fromJson(file, RetailAnalytics[].class);
                logger.info(arr.length + "");
                for (RetailAnalytics item : arr) {
                    set.add(item);
                }
            } catch (final Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
        logger.info(set.size() + "");
    }

    public static void main(String[] args) throws IOException, GitAPIException {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%r %d{ISO8601} [%t] %p %c %x - %m%n")));

        testGetAllVersions();
    }
}
