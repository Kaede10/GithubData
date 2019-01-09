package ReviewerRecommendation;

import ReviewerRecommendation.Algorithms.TIE.WVToolProcess;
import ReviewerRecommendation.DataProcess.DataPreparation;
import edu.udo.cs.wvtool.generic.vectorcreation.TFIDF;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Issue;

import java.util.*;

public class DataService {
    private DataPreparation dp = new DataPreparation();

    //存储按时间排序的pull request
    /*private List<PullRequest> prList = new ArrayList<PullRequest>();*/
    private List<Issue> issueList = new ArrayList<Issue>();

    //以prList中的元素顺序为键值，而非是pull request的号码
/*    private Map<Integer, List<String>> prReviewersFromCommitComment = new HashMap<Integer, List<String>>();   
    private Map<Integer, List<Pair>> CommitCommentContribution = new HashMap<Integer, List<Pair>>();*/
    private Map<Integer, List<String>> prReviewersFromIssueComment = new HashMap<Integer, List<String>>();
    private Map<Integer, List<Pair>> IssueCommentContribution = new HashMap<Integer, List<Pair>>();


    public DataPreparation getDp() {
        return dp;
    }

/*    public List<PullRequest> getPrList() {
        return prList;
    }*/
    
    public List<Issue> getPrList() {
        return issueList;
    }

    /*
    *  该方法应该首先被调用。对数据进行预处理
     */
    public void preProcessData(String owner, String repo) {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // 选择已关闭或者已合并的pull request
        dp.prepare(owner+"-"+repo);

        /*Map<String, PullRequest> prs = dp.getPrs();*/        

/*        for (Map.Entry<String, PullRequest> each : prs.entrySet()) {
            if (each.getValue().getState().equals("closed") ||
                    each.getValue().isMerged())
                prList.add(each.getValue());
        }

        System.out.println("prList length: " + prList.size());

        Collections.sort(prList, new Comparator<PullRequest>() {
            @Override
            public int compare(PullRequest o1, PullRequest o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        });*/
        Map<String, Issue> prs = dp.getIssues();
        for (Map.Entry<String, Issue> each : prs.entrySet()) {
            if (each.getValue().getState().equals("closed") ||
                    each.getValue().isMerged())
            	issueList.add(each.getValue());
        }

        System.out.println("prList length: " + issueList.size());

        Collections.sort(issueList, new Comparator<Issue>() {
            @Override
            public int compare(Issue o1, Issue o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        });

        //获得pull request 的评审者
        preProcessPRCodeReviewers();

        //过滤掉部分pull request
        filterData();

        //计算贡献
        preProcessContribution();
    }

    /*
   *  提供 计算某个PR的评审者 的服务
    */
    public List<String> getCodeReviewers(int rp) {
        /*if (prReviewersFromCommitComment.get(rp) == null
                && prReviewersFromIssueComment.get(rp) == null)
            return new ArrayList<String>();*/
    	
    	if (prReviewersFromIssueComment.get(rp) == null)
            return new ArrayList<String>();

        Set<String> reviewers = new HashSet<String>();
        /*reviewers.addAll(prReviewersFromCommitComment.get(rp));*/
        reviewers.addAll(prReviewersFromIssueComment.get(rp));
        String author = "";
        if (prList.get(rp).getUser() != null)
            author = prList.get(rp).getUser().getLogin();
        if (reviewers.contains(author))
            reviewers.remove(author);

        List<String> ret = new ArrayList<String>(reviewers);
        return ret;
    }

    /*
    *  提供 计算某个PR的各个评审者以及其贡献  的服务
     */
    public Map<String, List<Pair>> getReviewerContribution(int i) {
        List<Pair> pairs1 = IssueCommentContribution.get(i);
        /*List<Pair> pairs2 = CommitCommentContribution.get(i);*/
        Map<String, List<Pair>> ret = new HashMap<String, List<Pair>>();

        extractContribution(pairs1, ret);
        /*extractContribution(pairs2, ret);*/

        // 去除pull request作者的数据
        String author = "";
        if (prList.get(i).getUser() != null)
            author = prList.get(i).getUser().getLogin();
        ret.remove(author);

        return ret;
    }

    private void preProcessPRCodeReviewers() {
        for (int i = 0; i < prList.size(); i++) {

            String author = "";
            if (prList.get(i).getUser() != null)
                author = prList.get(i).getUser().getLogin();

            PullRequest pr = prList.get(i);
            List<CommitComment> prComments = dp.getPrComments().get(pr.getNumber()+"");
            List<Comment> issueComments = dp.getIssueComments().get(pr.getNumber()+"");

            int flag = 0;
            /*if (prComments == null) {
                prReviewersFromCommitComment.put(i, null);
                flag ++;
            }*/
            if (issueComments == null) {
                prReviewersFromIssueComment.put(i, null);
                flag ++;
            }
            if (flag == 1) //flag == 2
                continue;

            List<String> reviewersFromCommitComment = new ArrayList<String>();
            List<String> reviewersFromIssueComment = new ArrayList<String>();
            if (prComments != null)
                for (CommitComment each : prComments) {
                    if (each.getUser() == null)
                        continue;
                    if (!each.getUser().getLogin().equals(author) &&
                            !reviewersFromCommitComment.contains(each.getUser().getLogin()))
                        reviewersFromCommitComment.add(each.getUser().getLogin());
                }

            if (issueComments != null)
                for (Comment each : issueComments) {
                    if (each.getUser() == null)
                        continue;
                    if (!each.getUser().getLogin().equals(author) &&
                            !reviewersFromIssueComment.contains(each.getUser().getLogin()))
                        reviewersFromIssueComment.add(each.getUser().getLogin());
                }

            /*prReviewersFromCommitComment.put(i, reviewersFromCommitComment);*/
            prReviewersFromIssueComment.put(i, reviewersFromIssueComment);
        }
    }

/*    private List<CommitComment> getCommitComments(int i) {
        return dp.getPrComments().get(prList.get(i).getNumber()+"");
    }*/

    private List<Comment> getIssueComments(int i) {
        return dp.getIssueComments().get(prList.get(i).getNumber()+"");
    }

    private void preProcessContribution() {
        for (int i = 0; i < prList.size(); i++) {
            /*List<CommitComment> ccs = getCommitComments(i);
            if (ccs != null) {
                List<Pair> pairs1 = new ArrayList<Pair>();
                for (CommitComment cc : ccs) {
                    if (cc.getUser() == null) continue;
                    String reviewer = cc.getUser().getLogin();
                    String path = cc.getPath();
                    String commentBody = cc.getBody();
                    Date date = cc.getCreatedAt();
                    pairs1.add(new Pair(reviewer, path, commentBody, date));
                }
                CommitCommentContribution.put(i, pairs1);
            }*/

            List<Comment> comments = getIssueComments(i);
            if (comments != null) {
                List<Pair> pairs2 = new ArrayList<Pair>();
                for (Comment com : comments) {
                    if (com.getUser() == null) continue;
                    String reviewer = com.getUser().getLogin();
                    String commentBody = com.getBody();
                    Date date = com.getCreatedAt();
                    pairs2.add(new Pair(reviewer, commentBody, date));
                }
                IssueCommentContribution.put(i, pairs2);
            }
        }
    }

    private void extractContribution(List<Pair> pairs, Map<String, List<Pair>> ret) {
        for (Pair each : pairs) {
            if (ret.containsKey(each.getReviewer()))
                ret.get(each.getReviewer()).add(each);
            else {
                List<Pair> tmp = new ArrayList<Pair>();
                tmp.add(each);
                ret.put(each.getReviewer(), tmp);
            }
        }
    }


    private void filterData() {
        Set<Integer> removal = new HashSet<Integer>();

        Map<String, Map<Integer, Double>>  wordVector = null;
        WVToolProcess wvtp = new WVToolProcess(new TFIDF());
        try {
            wvtp.process("pr", prList.size(), this);
            wordVector = wvtp.getWordVector();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < prList.size(); i++) {
            if (getCodeReviewers(i) == null || getCodeReviewers(i).size() < 2) {
                removal.add(i);
            }
            if (wordVector.get(prList.get(i).getNumber()+"").size() < 5)
                removal.add(i);
        }

        List<PullRequest> prListTmp = new ArrayList<PullRequest>();
        Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();

        for (int i = 0; i < prList.size(); i++) {
            if (!removal.contains(i)) {
                prListTmp.add(prList.get(i));
                indexMap.put(prListTmp.size()-1, i);
            }
            else {
                /*prReviewersFromCommitComment.remove(i);*/
                prReviewersFromIssueComment.remove(i);
            }
        }

       /* Map<Integer, List<String>> ccReviewers = new HashMap<Integer, List<String>>();*/
        Map<Integer, List<String>> icReviewers = new HashMap<Integer, List<String>>();

        for (Map.Entry<Integer, Integer> each : indexMap.entrySet()) {
            /*List<String> val = prReviewersFromCommitComment.get(each.getValue());
            ccReviewers.put(each.getKey(), val);*/

            List<String> val1 = prReviewersFromIssueComment.get(each.getValue());
            icReviewers.put(each.getKey(), val1);
        }

        /*prReviewersFromCommitComment = ccReviewers;*/
        prReviewersFromIssueComment = icReviewers;
        prList = prListTmp;

        System.out.println("Filtered Prs: " + prList.size());
    }

    public static class Pair {
        String reviewer;
        String path;
        String comment;
        Date date;
        public String getReviewer() {
            return reviewer;
        }
        public String getPath() {
            return path;
        }
        public Date getDate() {
            return date;
        }
        public Pair(String rev, String path, String com, Date date) {
            this.reviewer = rev;
            this.path = path;
            this.comment = com;
            this.date = date;
        }
        public Pair(String rev, String com, Date date) {
            this.reviewer = rev;
            this.comment = com;
            this.date = date;
        }
    }
}
