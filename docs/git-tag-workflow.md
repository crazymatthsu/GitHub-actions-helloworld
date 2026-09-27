# Git tag image publish

A pushed git tag whose name starts with `v` triggers `.github/workflows/publish-image.yml` (`Publish Docker image`). That workflow runs the JUnit build, then publishes the image to GHCR.

```yaml
on:
  push:
    branches:
      - main
    tags:
      - "v*"
  workflow_dispatch:
```

## Create and push a tag

The tag is created on the current commit. The name has to start with `v`. Pushing the tag is what starts the workflow. Creating it only on your machine does nothing.

```shell
git tag v1.0.0
git push origin v1.0.0
```

A release candidate uses the same pattern:

```shell
git tag v1.2.3-rc-1
git push origin v1.2.3-rc-1
```

`git push origin v1.2.3` matches the filter. `git push origin 1.2.3` does not, because the name has to start with `v`.

## What gets published

`docker/metadata-action` tags the image from the git ref. A tag push publishes that git tag name and the commit SHA, for example:

- `ghcr.io/crazymatthsu/github-actions-helloworld:v1.2.3`
- `ghcr.io/crazymatthsu/github-actions-helloworld:sha-74ef238`

The metadata action also adds `latest` for a version tag. One push can attach several tags to the same image digest.

GHCR does not enforce semantic versioning. An image tag is any valid Docker name. `v1.2.3-rc-5` and `v1.2.3-rc-74ef238` are already published as image tags if you push git tags with those names, because `type=ref,event=tag` copies the git tag and `v*` matches both.

A GitHub run number is not a git tag, so it is not added today. To publish that as well, add another tag in the metadata step, for example `v1.2.3-rc-${{ github.run_number }}`, next to the existing `type=ref,event=tag` and `type=sha` lines. All of those names then point at one digest.

## Tests still run

`.github/workflows/build-and-test.yml` ignores every tag on its own `push` trigger, so a tag push does not start that workflow by itself. The publish workflow calls it with `workflow_call`, so the tests still run before the image is pushed.

The package page for images from this repo is https://github.com/crazymatthsu/GitHub-actions-helloworld/pkgs/container/github-actions-helloworld.
