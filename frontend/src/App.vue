<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 提供全局中文语言配置
provide('locale', zhCN)
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* Google Fonts - Notion Style (Inter) */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

/* 全局CSS变量 - Notion 风格配色方案 */
:root {
  /* 品牌色 - 紫色系 (Notion Primary) */
  --color-primary: #7C3AED;
  --color-primary-light: #8B5CF6;
  --color-primary-dark: #6D28D9;
  --color-secondary: #191919;
  --color-secondary-light: #2D2D2D;

  /* 功能色 */
  --color-cta: #7C3AED;
  --color-success: #16A34A;
  --color-warning: #EAB308;
  --color-error: #EF4444;
  --color-info: #3B82F6;

  /* 中性色 (Notion Surface) */
  --color-background: #FFFFFF;
  --color-background-secondary: #F7F7F5;
  --color-background-tertiary: #F1F1EF;
  --color-background-dark: #191919;
  --color-text: #37352F;
  --color-text-secondary: #787774;
  --color-text-muted: #B4B4B0;
  --color-border: #E9E9E7;
  --color-border-light: #F1F1EF;

  /* 渐变 */
  --gradient-primary: linear-gradient(135deg, #7C3AED 0%, #6D28D9 100%);
  --gradient-hero: linear-gradient(180deg, #F0EBFF 0%, #FFFFFF 100%);
  --gradient-hero-dark: linear-gradient(180deg, #191919 0%, #0A0A0A 100%);

  /* 纯色背景 */
  --bg-primary: #7C3AED;
  --bg-primary-hover: #6D28D9;
  --bg-dark: #191919;
  --bg-dark-hover: #2D2D2D;
  --bg-success: #16A34A;

  /* 阴影 (Notion 4-Level Elevation) */
  --shadow-sm: rgba(15, 15, 15, 0.04) 0px 1px 2px 0px;
  --shadow-md: rgba(15, 15, 15, 0.08) 0px 4px 12px 0px;
  --shadow-lg: rgba(15, 15, 15, 0.20) 0px 24px 48px -8px;
  --shadow-xl: rgba(15, 15, 15, 0.16) 0px 16px 48px -8px;
  --shadow-card: rgba(15, 15, 15, 0.04) 0px 1px 2px 0px;
  --shadow-card-hover: 0 8px 24px rgba(124, 58, 237, 0.12);
  --shadow-green: 0 4px 14px rgba(124, 58, 237, 0.2);

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-2xl: 24px;
  --radius-full: 9999px;

  /* 过渡 */
  --transition-fast: 150ms ease-out;
  --transition-normal: 200ms ease-out;
  --transition-slow: 300ms ease-out;

  /* 毛玻璃 */
  --glass-bg: rgba(255, 255, 255, 0.9);
  --glass-bg-green: rgba(240, 235, 255, 0.8);
  --glass-blur: blur(12px);

  /* Notion 柔和色彩卡片背景 */
  --tint-peach: #FFF0E6;
  --tint-rose: #FFE4EE;
  --tint-mint: #E6FFF0;
  --tint-lavender: #F0E6FF;
  --tint-sky: #E6F4FF;
  --tint-yellow: #FFF9E6;
  --tint-yellow-bold: #FFD600;
  --tint-cream: #FBF9F6;
  --tint-gray: #F1F1EF;
}

/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: 1.55;
}

html {
  overflow-x: hidden;
}

h1, h2, h3, h4, h5, h6 {
  font-family: 'Inter', sans-serif;
  font-weight: 600;
}

/* 全局链接样式 */
a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-primary);
}

/* 全局按钮增强 */
.ant-btn {
  transition: all var(--transition-normal);
  font-weight: 500;
  border-radius: var(--radius-md);
}

.ant-btn-primary {
  background: var(--bg-primary);
  border-color: var(--bg-primary);
  box-shadow: var(--shadow-green);
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--bg-primary-hover);
  border-color: var(--bg-primary-hover);
  box-shadow: 0 6px 20px rgba(124, 58, 237, 0.3);
}

/* 全局卡片增强 */
.ant-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  transition: all var(--transition-normal);
}

/* 可点击元素统一cursor */
.clickable,
[role="button"],
button,
.ant-btn,
.ant-card.hoverable {
  cursor: pointer;
}

/* 选中状态 */
::selection {
  background: rgba(124, 58, 237, 0.2);
  color: var(--color-text);
}

/* Ant Design 菜单覆盖 */
.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--color-primary) !important;
}

.ant-menu-horizontal > .ant-menu-item:hover,
.ant-menu-horizontal > .ant-menu-item-active {
  color: var(--color-text) !important;
}

/* Ant Design 按钮 disabled 状态 */
.ant-btn:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
  border-color: var(--color-border) !important;
  box-shadow: none !important;
}

.ant-btn-primary:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
}

/* Steps 组件样式覆盖 */
.ant-steps .ant-steps-item-process .ant-steps-item-icon {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon {
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon > .ant-steps-icon {
  color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish > .ant-steps-item-container > .ant-steps-item-tail::after {
  background-color: var(--color-primary);
}

/* 输入框聚焦状态 */
.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(124, 58, 237, 0.1) !important;
}

.ant-input:hover {
  border-color: var(--color-primary-light);
}

/* 文本域 */
.ant-input-textarea-show-count::after {
  color: var(--color-text-muted);
}

/* Progress 组件 */
.ant-progress-bg {
  background: var(--gradient-primary);
}

/* Spin 组件 */
.ant-spin-dot-item {
  background-color: var(--color-primary);
}
</style>
