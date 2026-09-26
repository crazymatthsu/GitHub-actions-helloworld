# GitHub workflow commands

Repository: `crazymatthsu/GitHub-actions-helloworld`

The publish workflow is `.github/workflows/publish-image.yml` (`Publish Docker image`). A push to `main` runs the JUnit build, then publishes the image to GHCR. The same workflow can also be started by hand.

## Trigger

A push to `main` is what started the runs. That is a git command, not `gh`:

```shell
git push origin main
```

Pushes that did this:

- `6f20be4` — run [36275552836](https://github.com/crazymatthsu/GitHub-actions-helloworld/actions/runs/36275552836)
- `85b43c4` — run [36276189024](https://github.com/crazymatthsu/GitHub-actions-helloworld/actions/runs/36276189024)

To start the same workflow without a new commit, use `workflow_dispatch`:

```shell
gh workflow run "Publish Docker image" --repo crazymatthsu/GitHub-actions-helloworld --ref main
```

## Monitor

List workflows:

```shell
gh workflow list --repo crazymatthsu/GitHub-actions-helloworld
```

Find the run created by a push:

```shell
gh run list --repo crazymatthsu/GitHub-actions-helloworld --limit 10 \
  --json databaseId,status,name,event,headSha,url,conclusion,displayTitle,createdAt
```

Read one run, including each job:

```shell
gh run view 36275552836 --repo crazymatthsu/GitHub-actions-helloworld \
  --json status,conclusion,jobs,url,event,headSha,displayTitle
```

Read the log:

```shell
gh run view 36275552836 --repo crazymatthsu/GitHub-actions-helloworld --log
```

While a run is still going, poll it until a job fails or the run completes:

```shell
gh run view 36276189024 --repo crazymatthsu/GitHub-actions-helloworld \
  --json status,conclusion,jobs
```

That view command is what the watcher repeats every 30 seconds. It prints nothing until the run succeeds or a job fails.
