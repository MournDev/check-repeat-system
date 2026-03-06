# 🔧 CheckMonitor.vue 集成实时推送 - 修改指南

## 📝 需要修改的位置

### 1. 导入部分（第 162-170 行）

**原代码**:
```javascript
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Refresh, DataLine, Tickets, Delete, InfoFilled, SuccessFilled,
  Check, Loading, Clock, Document, Files, Connection, DocumentChecked
} from '@element-plus/icons-vue'
import { getCheckStatus, subscribeCheckStatus } from '@/api/student'
```

**修改为**:
```javascript
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Refresh, DataLine, Tickets, Delete, InfoFilled, SuccessFilled,
  Check, Loading, Clock, Document, Files, Connection, DocumentChecked
} from '@element-plus/icons-vue'
import { getCheckStatus, subscribeCheckStatus, getCheckTaskById } from '@/api/student'
import { useCheckProgress } from '@/composables/useCheckProgress'
```

---

### 2. 添加 Hook 使用（第 200 行后）

**在 `const logContainer = ref(null)` 后添加**:
```javascript
// 使用实时推送 Hook
const { connect, disconnect, progress: checkProgress, isConnected } = useCheckProgress();
```

---

### 3. 修改 connectWebSocket 方法（第 358-371 行）

**原代码**:
```javascript
const connectWebSocket = () => {
  try {
    websocket.value = subscribeCheckStatus(route.params.paperId, (data) => {
      if (data.type === 'status_update') {
        updateStatus(data.data)
        addLog('info', data.data.message || '状态更新')
      }
    })
  } catch (error) {
    console.error('WebSocket 连接失败:', error)
    // 降级到轮询
    startPolling()
  }
}
```

**修改为**:
```javascript
const connectWebSocket = () => {
  const taskId = route.params.taskId;
  
  // 使用新的 Hook 连接
  connect(taskId);
  
  // 监听进度变化
  watch(() => checkProgress.stage, (newStage) => {
    if (newStage) {
      updateStatusFromHook(checkProgress);
      addLog('info', checkProgress.message || '状态更新');
      
      if (newStage === 'COMPLETED') {
        ElMessage.success('查重完成！');
        setTimeout(() => {
          router.push(`/student/plagiarism-report/${checkProgress.paperId}`);
        }, 2000);
      } else if (newStage === 'FAILED') {
        ElMessage.error(checkProgress.message || '查重失败');
      }
    }
  });
};
```

---

### 4. 修改 disconnectWebSocket 方法（第 373-378 行）

**原代码**:
```javascript
const disconnectWebSocket = () => {
  if (websocket.value) {
    websocket.value.close()
    websocket.value = null
  }
}
```

**修改为**:
```javascript
const disconnectWebSocket = () => {
  disconnect();
};
```

---

### 5. 添加新的辅助方法（第 400 行后，addLog 方法前）

**添加**:
```javascript
/**
 * 从 Hook 更新状态
 */
const updateStatusFromHook = (progress) => {
  currentStatus.value = {
    ...currentStatus.value,
    overallProgress: progress.percent,
    currentStage: mapStageToInternal(progress.stage),
    estimatedRemainingTime: (progress.estimatedRemainingSeconds || 0) * 1000
  };
  
  // 更新阶段状态
  checkStages.value = checkStages.value.map(stage => {
    if (stage.key === mapStageToInternal(progress.stage)) {
      return {
        ...stage,
        status: 'processing',
        progress: progress.percent
      };
    }
    if (isStageCompleted(stage.key, progress.stage)) {
      return {
        ...stage,
        status: 'completed',
        progress: 100
      };
    }
    return stage;
  });
};

/**
 * 映射阶段名称
 */
const mapStageToInternal = (stage) => {
  const mapping = {
    'FILE_PARSING': 'upload',
    'TEXT_COMPARING': 'internal_check',
    'REPORT_GENERATING': 'report_generation',
    'COMPLETED': 'completed'
  };
  return mapping[stage] || 'internal_check';
};

/**
 * 判断阶段是否完成
 */
const isStageCompleted = (stageKey, currentStage) => {
  const stageOrder = ['upload', 'internal_check', 'report_generation', 'completed'];
  const currentIndex = stageOrder.indexOf(currentStage);
  const targetIndex = stageOrder.indexOf(stageKey);
  return currentIndex > targetIndex;
};
```

---

### 6. 修改生命周期（第 420-429 行）

**原代码**:
```javascript
onMounted(() => {
  connectWebSocket()
  // 添加初始日志
  addLog('info', '连接到查重服务...')
})

onUnmounted(() => {
  disconnectWebSocket()
  stopPolling()
})
```

**修改为**:
```javascript
onMounted(() => {
  connectWebSocket();
  // 添加初始日志
  addLog('info', '连接到查重服务...');
});

onUnmounted(() => {
  disconnectWebSocket();
  stopPolling();
});
```

---

## ✅ 验证清单

修改完成后，请检查：

- [ ] 所有 import 语句正确
- [ ] useCheckProgress Hook 已引入
- [ ] connectWebSocket 方法已更新
- [ ] disconnectWebSocket 方法已简化
- [ ] updateStatusFromHook 方法已添加
- [ ] mapStageToInternal 方法已添加
- [ ] isStageCompleted 方法已添加
- [ ] 生命周期钩子正确调用

---

## 🎯 预期效果

修改后的功能：

1. **实时进度推送**
   - 使用 STOMP 协议接收后端消息
   - 自动重连机制
   - 组件卸载自动清理

2. **状态同步更新**
   - 进度条实时更新
   - 阶段指示器同步变化
   - 日志信息动态添加

3. **完成后自动跳转**
   - 检测到 COMPLETED 状态
   - 显示成功提示
   - 2 秒后跳转到报告页面

---

## 🐛 常见问题

### Q1: 提示 `useCheckProgress` 未定义
**解决**: 确认已安装依赖并正确导入

### Q2: 连接后无反应
**解决**: 检查 taskId 是否正确，查看 Console 日志

### Q3: 阶段映射错误
**解决**: 检查 mapStageToInternal 方法的映射关系

---

## 📚 相关文档

- `docs/前端实时推送集成指南.md` - 详细教程
- `docs/快速验证与故障排查.md` - 问题诊断

祝您修改顺利！🚀
