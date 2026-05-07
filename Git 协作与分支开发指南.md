# 🚀论坛团队 Git 协作与分支开发指南

为了保证项目的顺利推进，避免代码冲突和覆盖问题，本项目采用标准的 **Git Flow** 工作流。

请团队全体成员（包括需求分析、系统设计、核心编码和软件测试同学）务必仔细阅读并严格遵守本指南。

## 🌳 1. 分支架构说明

我们仓库的分支分为两类：**长期分支** 和 **临时分支**。

### 长期分支（全生命周期存在）

- **`main`（受保护的生产分支）**
  - **作用：** 存放随时可发布到生产环境的稳定代码。
  - **权限：** **绝对禁止**任何人在本地直接推送 (`push`) 代码到此分支。所有代码必须通过 Pull Request (PR) 合并。
- **`develop`（开发集成分支）**
  - **作用：** 团队日常开发的主战场。所有新功能开发完毕后，都会合并到这里进行集成测试。

### 临时分支（开发完并合并后即删除）

- **`feature/\*`（功能分支）**
  - **作用：** 用于开发新的功能模块。
  - **命名示例：** `feature/cas-auth`（CAS认证）、`feature/flea-market`（跳蚤市场）、`feature/academic-module`（学术交流）、`feature/anonymous-help`（匿名互助）。
- **`bugfix/\*`（常规修复分支）**
  - **作用：** 用于修复测试阶段在 `develop` 分支上发现的 Bug。
  - **命名示例：** `bugfix/item-status-machine-fix`。
- **`hotfix/\*`（紧急热修复分支）**
  - **作用：** 仅用于修复已经上线到 `main` 分支的紧急严重漏洞。
  - **命名示例：** `hotfix/security-patch`。

------

## 💻 2. 标准开发工作流 (核心必读)

## 首次拉取项目执行以下命令来同步所有分支：

Bash

```
git clone git@github.com:Asuka-rei0/WHUforum.git
git fetch --all
```

------

## 当你准备开始编写一个新模块（例如：CAS 认证模块）时，请严格按照以下 5 个步骤操作：

### Step 1: 同步最新的集成代码

永远不要基于过时的代码开发。首先切回 `develop` 分支，并拉取云端最新代码：

Bash

```
git checkout develop
git pull origin develop
```

### Step 2: 创建并切换到你的专属功能分支

基于最新的 `develop`，建立你的功能分支：

Bash

```
git checkout -b feature/cas-auth
```

*(注意替换成你实际开发的模块名称)*

### Step 3: 在本地愉快地写代码并提交

完成一部分代码编写后，提交到本地暂存区：

Bash

```
git add .
git commit -m "feat: 完成 CAS 认证模块的登录接口"
```

> **💡 提交信息规范 (Commit Message):** 请使用带有前缀的提交信息，方便团队回溯日志：
>
> - `feat:` 新增功能 (Feature)
> - `fix:` 修复 Bug
> - `docs:` 修改文档 (如 README, SDP, SRS 等)
> - `refactor:` 代码重构 (不改变接口行为)

### Step 4: 将功能分支推送到云端仓库

当你完成了一天的开发，或者该模块全部写完后，将其推送到 GitHub：

Bash

```
# 第一次推送该分支时需要加 -u 绑定
git push -u origin feature/cas-auth

# 以后在这个分支上只需执行：
git push
```

### Step 5: 发起 Pull Request (PR) 申请合并

功能开发完成并自测通过后，**不要在本地自行合并**！

1. 登录 GitHub 网页端，进入仓库。
2. 系统通常会弹出一个绿色的按钮提示 `Compare & pull request`，点击它。
3. 确保合并方向是：**`feature/cas-auth`  ➡  `develop`**。
4. 填写本次更新的描述，提交 PR。
5. 通知负责系统设计或测试的同学进行 **Code Review (代码审查)**。
6. 审核通过后，由项目组长点击 Merge 合并入 `develop`。

------

## 🚫 3. 团队开发“三条红线”

1. **严禁越级推送：** 任何人不得在本地直接将代码 `push` 到 `main` 分支。
2. **严禁在 `develop` 直接修 Bug：** 发现 Bug 时，请基于 `develop` 切出一个 `bugfix/*` 分支进行修复，然后通过 PR 合并回去。
3. **遇到冲突不要慌：** 如果在提交 PR 时提示代码冲突 (Conflict)，不要强行覆盖。请立刻在群里沟通，找到产生冲突的代码提交者，两人一起在本地解决冲突后再重新提交。

------

### 🔧 附录：环境配置说明

为了保证国内网络下能顺畅推送代码，团队成员请确保本地已配置好 Git 的 SSH 密钥，并使用 SSH 地址克隆仓库：

Bash

```
git clone git@github.com:Asuka-rei0/WHUforum.git
```

*祝 校园论坛开发顺利！* 🚀