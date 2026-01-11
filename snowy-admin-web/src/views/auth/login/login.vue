<template>
	<div class="iot-login-wrapper">
		<!-- 左侧背景区域 -->
		<div class="iot-login-left">
			<div class="iot-particles"></div>
			<div class="iot-content">
				<div class="iot-logo-section">
					<div class="iot-logo-circle">
						<img :alt="systemName" :src="sysBaseConfig.SNOWY_SYS_LOGO" class="iot-logo-img" />
					</div>
					<h1 class="iot-system-name">{{ systemName }}</h1>
					<p class="iot-system-desc">{{ sysBaseConfig.SNOWY_SYS_DEFAULT_DESCRRIPTION }}</p>
				</div>

				<div class="iot-features">
					<div class="iot-feature-item">
						<div class="iot-feature-icon">
							<api-outlined />
						</div>
						<div class="iot-feature-text">
							<h3>设备接入</h3>
							<p>多协议设备统一接入管理</p>
						</div>
					</div>
					<div class="iot-feature-item">
						<div class="iot-feature-icon">
							<cloud-server-outlined />
						</div>
						<div class="iot-feature-text">
							<h3>边缘计算</h3>
							<p>本地数据处理与分析</p>
						</div>
					</div>
					<div class="iot-feature-item">
						<div class="iot-feature-icon">
							<security-scan-outlined />
						</div>
						<div class="iot-feature-text">
							<h3>安全可靠</h3>
							<p>国密算法数据加密传输</p>
						</div>
					</div>
				</div>

				<div class="iot-footer">
					<p>{{ sysBaseConfig.SNOWY_SYS_COPYRIGHT }} {{ sysBaseConfig.SNOWY_SYS_VERSION }}</p>
				</div>
			</div>
		</div>

		<!-- 右侧登录表单区域 -->
		<div class="iot-login-right">
			<div class="iot-lang-switch">
				<a-dropdown>
					<global-outlined class="iot-lang-icon" />
					<template #overlay>
						<a-menu>
							<a-menu-item
								v-for="item in lang"
								:key="item.value"
								:command="item"
								:class="{ selected: config.lang === item.value }"
								@click="configLang(item.value)"
							>
								{{ item.name }}
							</a-menu-item>
						</a-menu>
					</template>
				</a-dropdown>
			</div>

			<div class="iot-login-form-container">
				<div class="iot-login-card">
					<div class="iot-login-header">
						<h2>{{ $t('login.signInTitle') }}</h2>
						<p class="iot-login-subtitle">边缘网关管理系统</p>
					</div>
					<a-form ref="loginForm" :model="ruleForm" :rules="rules">
						<a-form-item name="account">
							<a-input
								v-model:value="ruleForm.account"
								:placeholder="$t('login.accountPlaceholder')"
								size="large"
								@keyup.enter="login"
							>
								<template #prefix>
									<UserOutlined class="login-icon-gray" />
								</template>
							</a-input>
						</a-form-item>
						<a-form-item name="password">
							<a-input-password
								v-model:value="ruleForm.password"
								:placeholder="$t('login.PWPlaceholder')"
								size="large"
								autocomplete="off"
								@keyup.enter="login"
							>
								<template #prefix>
									<LockOutlined class="login-icon-gray" />
								</template>
							</a-input-password>
						</a-form-item>
						<a-form-item name="validCode" v-if="captchaOpen === 'true'">
							<a-row :gutter="8">
								<a-col :span="17">
									<a-input
										v-model:value="ruleForm.validCode"
										:placeholder="$t('login.validPlaceholder')"
										size="large"
										@keyup.enter="login"
									>
										<template #prefix>
											<verified-outlined class="login-icon-gray" />
										</template>
									</a-input>
								</a-col>
								<a-col :span="7">
									<img :src="validCodeBase64" class="login-validCode-img" @click="loginCaptcha" />
								</a-col>
							</a-row>
						</a-form-item>
						<a-form-item>
							<a-button type="primary" class="w-full" :loading="loading" round size="large" @click="login"
								>{{ $t('login.signIn') }}
							</a-button>
						</a-form-item>
					</a-form>
				</div>
			</div>
		</div>
	</div>
</template>
<script setup>
	import loginApi from '@/api/auth/loginApi'
	import smCrypto from '@/utils/smCrypto'
	import { required } from '@/utils/formRules'
	import { afterLogin } from './util'
	import configData from '@/config'
	import configApi from '@/api/dev/configApi'
	import tool from '@/utils/tool'
	import { globalStore, iframeStore, keepAliveStore, viewTagsStore } from '@/store'
	const { proxy } = getCurrentInstance()

	const captchaOpen = ref(configData.SYS_BASE_CONFIG.SNOWY_SYS_DEFAULT_CAPTCHA_OPEN_FLAG_FOR_B)
	const validCodeBase64 = ref('')
	const loading = ref(false)

	const ruleForm = reactive({
		account: 'bizAdmin',
		password: '123456',
		validCode: '',
		validCodeReqNo: '',
		autologin: false
	})

	const rules = reactive({
		account: [required(proxy.$t('login.accountError'), 'blur')],
		password: [required(proxy.$t('login.PWError'), 'blur')]
	})
	const lang = ref([
		{
			name: '简体中文',
			value: 'zh-cn'
		},
		{
			name: 'English',
			value: 'en'
		}
	])
	const config = ref({
		lang: tool.data.get('APP_LANG') || proxy.$CONFIG.LANG,
		theme: tool.data.get('APP_THEME') || 'default'
	})

	const store = globalStore()
	const kStore = keepAliveStore()
	const iStore = iframeStore()
	const vStore = viewTagsStore()

	const setSysBaseConfig = store.setSysBaseConfig
	const clearKeepLive = kStore.clearKeepLive
	const clearIframeList = iStore.clearIframeList
	const clearViewTags = vStore.clearViewTags

	const sysBaseConfig = computed(() => {
		return store.sysBaseConfig
	})
	const systemName = ref(sysBaseConfig.value.SNOWY_SYS_NAME)
	onMounted(() => {
		let formData = ref(configData.SYS_BASE_CONFIG)
		configApi
			.configSysBaseList()
			.then((data) => {
				if (data) {
					data.forEach((item) => {
						formData.value[item.configKey] = item.configValue
					})
					captchaOpen.value = formData.value.SNOWY_SYS_DEFAULT_CAPTCHA_OPEN_FLAG_FOR_B
					tool.data.set('SNOWY_SYS_BASE_CONFIG', formData.value)
					setSysBaseConfig(formData.value)
					refreshSwitch()
				}
			})
			.catch(() => {})
	})

	onBeforeMount(() => {
		clearViewTags()
		clearKeepLive()
		clearIframeList()
	})

	// 监听语言
	watch(
		() => config.value.lang,
		(newValue) => {
			proxy.$i18n.locale = newValue
			tool.data.set('APP_LANG', newValue)
		}
	)
	// 主题
	watch(
		() => config.value.theme,
		(newValue) => {
			document.body.setAttribute('data-theme', newValue)
		}
	)
	// 通过开关加载内容
	const refreshSwitch = () => {
		// 判断是否开启验证码
		if (captchaOpen.value === 'true') {
			// 加载验证码
			loginCaptcha()
			// 加入校验
			rules.validCode = [required(proxy.$t('login.validError'), 'blur')]
		}
	}
	// 获取验证码
	const loginCaptcha = () => {
		loginApi.getPicCaptcha().then((data) => {
			validCodeBase64.value = data.validCodeBase64
			ruleForm.validCodeReqNo = data.validCodeReqNo
		})
	}
	// 登录
	const loginForm = ref()
	const login = async () => {
		loginForm.value
			.validate()
			.then(async () => {
				loading.value = true
				const loginData = {
					account: ruleForm.account,
					// 密码进行SM2加密，传输过程中看到的只有密文，后端存储使用hash
					password: smCrypto.doSm2Encrypt(ruleForm.password),
					validCode: ruleForm.validCode,
					validCodeReqNo: ruleForm.validCodeReqNo
				}
				const loginToken = await loginApi.login(loginData)
				await afterLogin(loginToken)
			})
			.catch((err) => {
				console.log(err)
				loading.value = false
				if (captchaOpen.value === 'true') {
					loginCaptcha()
				}
			})
	}
	const configLang = (key) => {
		config.value.lang = key
	}
	// logo链接
	const handleLink = (e) => {
		if (!sysBaseConfig.value.SNOWY_SYS_COPYRIGHT_URL) {
			e?.stopPropagation()
			e?.preventDefault()
		}
	}
</script>
<style lang="less">
	@import 'login';
	.xn-color-0d84ff {
		color: #0d84ff;
	}
</style>
