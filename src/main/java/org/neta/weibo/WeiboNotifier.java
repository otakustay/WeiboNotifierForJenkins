package org.neta.weibo;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("UnusedDeclaration")
public class WeiboNotifier extends Notifier {

    private final String accessToken;

    @DataBoundConstructor
    public WeiboNotifier(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("access_token:" + getAccessToken());

        // Outputs project member -> sina weibo username mapping
        for (Map.Entry<String, String> entry : getDescriptor().getUserMap().entrySet()) {
            listener.getLogger().println(entry.getKey() + " " + entry.getValue());
        }

        listener.getLogger().println("build result: " + determineBuildResult(build));

        listener.getLogger().println("weibo notifier: done");

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    private static BuildResult determineBuildResult(Run build) {
        Result result = build.getResult();
        Run previousBuild = build.getPreviousBuild();
        if (previousBuild == null) {
            return result.isBetterOrEqualTo(Result.SUCCESS) ? BuildResult.SUCCESS : BuildResult.FAIL;
        }

        Result previousResult = previousBuild.getResult();
        if (previousResult.isBetterOrEqualTo(Result.SUCCESS)) {
            // Success -> Success = Success
            // Success -> Fail = Fail
            return result.isBetterOrEqualTo(Result.SUCCESS) ? BuildResult.SUCCESS : BuildResult.FAIL;
        }
        else {
            // Fail -> Fail = ContinuousFail
            // Fail -> Success = Recover
            return result.isBetterOrEqualTo(Result.SUCCESS) ? BuildResult.RECOVER : BuildResult.CONTINUOUS_FAIL;
        }
    }

    private void publishWeiboStatus(String content) throws IOException {
        List<NameValuePair> form = new ArrayList<NameValuePair>();
        form.add(new BasicNameValuePair("access_token", getAccessToken()));
        form.add(new BasicNameValuePair("status", content));
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost("https://api.weibo.com/2/statuses/update.json");
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form);
            post.setEntity(entity);
            BasicResponseHandler handler = new BasicResponseHandler();
            String response = client.execute(post, handler);
            System.out.println(response);
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Descriptor for {@link WeiboNotifier}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/WeiboNotifier/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private Map<String, String> userMap;

        public DescriptorImpl() {
            super(WeiboNotifier.class);
            load();
        }

        public FormValidation doCheckAccessToken(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set your weibo access token");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Sina Weibo Notifier";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            userMap = new HashMap<String, String>();
            if (formData.has("user")) {
                try {
                    JSONArray users = formData.getJSONArray("user");
                    for (int i = 0; i < users.size(); i++) {
                        String memberName = users.getJSONObject(i).getString("memberName");
                        String weiboName = users.getJSONObject(i).getString("weiboName");
                        if (memberName.length() > 0 && weiboName.length() > 0) {
                            userMap.put(memberName, weiboName);
                        }
                    }
                }
                catch (JSONException ex) {
                    // Only one user mapping specified
                    JSONObject user = formData.getJSONObject("user");
                    String memberName = user.getString("memberName");
                    String weiboName = user.getString("weiboName");
                    if (memberName.length() > 0 && weiboName.length() > 0) {
                        userMap.put(memberName, weiboName);
                    }
                }
            }

            save();
            return super.configure(req,formData);
        }

        public Map<String, String> getUserMap() {
            return userMap;
        }
    }
}

