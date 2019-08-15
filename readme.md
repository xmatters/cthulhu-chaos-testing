# Cthulhu
Cthulhu is a Chaos Engineering tool that helps evaluating the resiliency of microservice systems.  It does that by simulating various chaos scenarios against a target infrastructure in a data-driven manner.

## Chaos Engineering
An ideal platform should be able to automatically detect failures and heal itself back to a normal state without any interruption of service.  Running chaos scenarios expose gaps in the self-healing ability of a platform.  Knowing about the short comings of the infrastruvcture allows engineering teams to become more efficient at recovering the system in the event of a disaster (either manually or by perfecting the self-healing features of the platform).

## Running from Gradle
The following command will run a given chaos scenario and output the log to the console.  See the module-specific 
instructions (below) on how to configure the container for them.  To specify re-usable configurations for Cthulhu, copy 
`./src/main/resources/application-overrides-template.properties` as `./src/main/resources/application-overrides.properties` 
1. `./gradlew bootRun < <path-to-scenario>`

## Building the Docker Image
1. `./gradlew prepareDocker -Penvironment=docker` — environment=docker excludes the application-overrides.properties from 
the jar file.
2. `docker build -t cthulhu .`

## Running from Docker 
The following command will run a given chaos scenario in a docker container, output the log to the console, and clean-up the container once the 
process has completed.  See the module-specific instructions (below) on how to configure the container for them.
```bash
   docker run -it --rm \
   -v <path-to-scenario>:/etc/cthulhu/scenario.yaml \
   cthulhu
```

#### Environment variable mapping
Environment variables can be used to define/overwrite configuration values using the following pattern 
`ABC_DEF --> abc.def`.

## Chaos Scenarios
Cthulhu executes Scenario files that contain a list of Chaos Events.  The Scenario files are in YAML format.  The 
following show the usage of all shared fields.  Refer to module-specific instructions for Chaos Events.

**Scenario**
* `name` — Name of the Scenario (used for logging).
* `chaosevents` — One or more Chaos Event entry:

**Chaos Event**
* `description` — Describes what this Chaos Event does (used for logging).
* `engine` — The target infrastructure engine (see module-specific instructions).
* `operation` — Operation to run (see module-specific instructions).
* `quantity` — Subset of the matched instances to affect.  The subset is taken randomly from the `targets`.  If missing, all matches are affected.
* `schedule` — Define delays and repetitions for the event (See the Schedule section).  If missing, the event will be executed once immediately.
* `skip` — Number of `targets` that will not be affected.  This can be used in conjunction with `schedule` to ensure a service is kept below its normal availability level, while ensuring it is not completely disabled.
* `target` — Expressions used by the Chaos Engine to identify instances to target.

**Schedule**
* `delay` — Delay before executing each occurrence of an event (e.g. 30s).
* `delayjitter` — Random amount of time added or removed from the delay (e.g. delay: 30s + delayjitter: 5s = 30s±5s).
* `initialdelay` — Delay before executing the first occurrence of an event (e.g. 10m).  If missing, delay is used.
* `repeat` — Number of repetition of this event.  If missing, the event will execute once.

## Global Configuration
* `cthulhu.event.timeout.default` — Default delay before cancelling Chaos Events.
    
## Amazon Web Services Configuration
There is no configuration specific to AWS in Cthulhu. [Configure the AWS console](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html#cli-quick-configuration)
with the account that will run chaos events.

#### AWS Chaos Events
* `engine` — Set to `aws-ec2`.
* `target` — Follows this pattern: `<zone>/<vm-name-tag>`.  Both Zone and VM Name supports regular expressions. Only VMs in the running state are considered for targets.

**Delete VMs**
* `operation` — Set to `delete`.

**Reset VMs**
* `operation` — Set to `reset`.

**Stop Vms**
* `operation` — Set to `stop`.

## Google Cloud Configuration
* `gcp.account.json` — Path to a Google Cloud credential file.
  * [How to create a Google Cloud Service Account](https://cloud.google.com/iam/docs/creating-managing-service-accounts).
  * To interact with VM instances, your service account must have the `Compute Admin` role.
* `gcp.project` — The name of the Google Cloud project which to connect.
  
#### Docker Configurations for GCP
* Passing-in a Google Cloud credential file: `-v ~/.ssh/sa_dev_chaostest.json:/etc/secrets/gcp-account.json`
* Specifying a GCP project: `-e GCP_PROJECT=<project-id>`

#### GCP Chaos Events
* `engine` — Set to `gcp-compute`.
* `target` — Follows this pattern: `<zone>/<vm-name>`.  Both Zone and VM Name supports regular expressions.

**Delete VMs**
* `operation` — Set to `delete`.

**Reset VMs**
* `operation` — Set to `reset`.

**Stop Vms**
* `operation` — Set to `stop`.
   
## Kubernetes Configuration
* `kube.config` — Path to a Kubernetes config.  This is generally `~/.kube/config` for the current user.  
  * [How get a K8s context for the Google Cloud Kubernetes Engine](https://cloud.google.com/kubernetes-engine/docs/quickstart).
  * To interact with containers, your service account must have the `Kubernetes Engine Admin` role.
    
#### Docker Configurations for K8s
* Passing-in a Kube config file: `-v ~/.kube/config:/etc/secrets/kube.config`

#### K8s Chaos Events
* `engine` — Set to `kubernetes`.
* `target` — Follows this pattern: `<namespace>/<pod-name>`.  Both Namespace and Pod Name supports regular expressions.

**Delete Pods**
* `operation` — Set to `delete`.

## Slack Notifications
* `slack.audit.filter` — List of audit types to include in the Slack notifications (see the auditing section).
* `slack.audit.<audit-type>.message` — Customize notifications per audit type.  The placeholder `%s` can be used to 
indicate where to put the original notification message.
* `slack.channels` — Override the default webhook channel.  Supports multiple values, using the syntax 
`#channelA #channelB`.
* `slack.icon_emoji` — Override the webhook avatar using an emoji (eg. `:smiling_imp:`).
* `slack.username` — Override the webhook's display name.
* `slack.webhook.url` — URL of a [Slack incoming webhook](https://api.slack.com/incoming-webhooks).
  
#### Docker Configurations for Slack Notifications
* Specifying a webhook URL: `-e SLACK_WEBHOOK_URL=https://hooks.slack.com/services/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`

## Building Additional Chaos Engines
Each Chaos Engine has its own sub-project, which has a dependency on the api project.  The main class must extends from
`com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine` and be marked with the annotation 
`com.xmatters.testing.cthulhu.api.annotations.EngineName`. The value of `EngineName` will match the `engine` field in 
Chaos Events.

Once a new Chaos Engine is created, a reference must be added in the main `build.gradle` file. 

Operation methods are public methods declared in the ChaosEngine, and marked with the annotation 
`com.xmatters.testing.cthulhu.api.annotations.OperationName`.  The value of `OperationName` will match the `operation` 
field in Chaos Events.  Operation methods must take an array parameter of the same type that is returned by the 
`getTargets` method.

The `T[] getTargets(ChaosEvent ev)` method must return an array of a concrete type.  All possible matches of a target 
must be returned.  `skip` and `quantity` are applied by the ChaosEventHandler before the final selection is then given 
to an operation method as parameter. 

## Building Additional Chaos Auditors
Each Chaos Auditor has its own sub-project, which has a dependency on the api project.  The main class must implements 
`com.xmatters.testing.cthulhu.api.auditing.ChaosAuditor`.  Additionally the same class can extend from 
`com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration` if it needs to configure itself.

Once a new Chaos Auditor is created, a reference must be added in the main `build.gradle` file.

# Disclaimer
Cthulhu is not an officially supported xMatters product.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

