// 获取URL参数中的帖子ID（用于编辑模式）
const postId = location.pathname.match(/\/edit\/(\d+)/)?.[1];
const userId = localStorage.getItem('userId');
const isEditMode = !!postId;

console.log('Post ID:', postId);
console.log('User ID:', userId);
console.log('Edit Mode:', isEditMode);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
  // 检查用户登录状态
  if (!userId) {
    showNotification('请先登录', 'error');
    setTimeout(() => {
      window.location.href = '/blog/login';
    }, 2000);
    return;
  }

  // 如果是编辑模式，加载帖子数据
  if (isEditMode) {
    loadPostForEdit();
  }

  // 初始化表单事件
  initializeForm();
  initializeImageUpload();
});

// 加载帖子数据用于编辑
async function loadPostForEdit() {
  try {
    const response = await fetch(`/blog/api/posts/${postId}`);
    if (!response.ok) {
      throw new Error('帖子不存在');
    }
    
    const post = await response.json();
    
    // 检查是否为作者本人
    if (!post.author || post.author.id != userId) {
      showNotification('无权限编辑此帖子', 'error');
      setTimeout(() => {
        window.history.back();
      }, 2000);
      return;
    }
    
    // 填充表单数据
    document.getElementById('titleInput').value = post.title || '';
    document.getElementById('contentTextarea').value = post.content || '';
    
    // 更新页面标题
    document.querySelector('.page-title').textContent = '编辑帖子';
    document.querySelector('.page-subtitle').textContent = '修改你的帖子内容';
    
    // 如果有图片，显示预览
    if (post.images && post.images.length > 0) {
      post.images.forEach(imageUrl => {
        addImagePreview(imageUrl);
      });
    }
    
  } catch (error) {
    console.error('加载帖子失败:', error);
    showNotification('加载帖子失败，请重试', 'error');
    setTimeout(() => {
      window.history.back();
    }, 2000);
  }
}

// 初始化表单事件
function initializeForm() {
  const form = document.getElementById('postEditForm');
  form.addEventListener('submit', handleFormSubmit);
}

// 处理表单提交
async function handleFormSubmit(e) {
  e.preventDefault();
  
  const title = document.getElementById('titleInput').value.trim();
  const content = document.getElementById('contentTextarea').value.trim();
  
  // 表单验证
  if (!title) {
    showNotification('请输入标题', 'error');
    return;
  }
  
  if (!content) {
    showNotification('请输入内容', 'error');
    return;
  }
  
  const submitBtn = document.getElementById('submitBtn');
  const originalText = submitBtn.textContent;
  submitBtn.textContent = isEditMode ? '保存中...' : '发布中...';
  submitBtn.disabled = true;
  
  try {
    // 准备请求数据
    const postData = {
      title: title,
      content: content,
      author: { id: parseInt(userId) }
    };
    
    // 收集图片URLs
    const imageElements = document.querySelectorAll('.image-preview img');
    if (imageElements.length > 0) {
      postData.images = Array.from(imageElements).map(img => img.src);
    }
    
    // 发送请求
    const url = isEditMode ? `/blog/api/posts/${postId}` : '/blog/api/posts';
    const method = isEditMode ? 'PUT' : 'POST';
    
    const response = await fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(postData)
    });
    
    if (response.ok) {
      const result = await response.json();
      showNotification(isEditMode ? '帖子更新成功' : '帖子发布成功', 'success');
      
      // 跳转到帖子详情页
      setTimeout(() => {
        const targetPostId = isEditMode ? postId : result.id;
        window.location.href = `/blog/post/${targetPostId}`;
      }, 1500);
    } else {
      throw new Error('请求失败');
    }
    
  } catch (error) {
    console.error('提交失败:', error);
    showNotification(isEditMode ? '更新失败，请重试' : '发布失败，请重试', 'error');
  } finally {
    submitBtn.textContent = originalText;
    submitBtn.disabled = false;
  }
}

// 初始化图片上传功能
function initializeImageUpload() {
  const uploadSection = document.getElementById('imageUploadSection');
  const fileInput = document.getElementById('imageFileInput');
  const uploadBtn = document.getElementById('uploadBtn');
  
  // 点击上传区域触发文件选择
  uploadSection.addEventListener('click', () => {
    fileInput.click();
  });
  
  // 点击上传按钮触发文件选择
  uploadBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    fileInput.click();
  });
  
  // 文件选择处理
  fileInput.addEventListener('change', handleFileSelect);
  
  // 拖拽上传支持
  uploadSection.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadSection.style.borderColor = 'rgba(102, 192, 244, 0.7)';
    uploadSection.style.background = 'rgba(27, 40, 56, 0.9)';
  });
  
  uploadSection.addEventListener('dragleave', (e) => {
    e.preventDefault();
    uploadSection.style.borderColor = 'rgba(102, 192, 244, 0.3)';
    uploadSection.style.background = 'rgba(27, 40, 56, 0.5)';
  });
  
  uploadSection.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadSection.style.borderColor = 'rgba(102, 192, 244, 0.3)';
    uploadSection.style.background = 'rgba(27, 40, 56, 0.5)';
    
    const files = e.dataTransfer.files;
    handleFiles(files);
  });
}

// 处理文件选择
function handleFileSelect(e) {
  const files = e.target.files;
  handleFiles(files);
}

// 处理文件
function handleFiles(files) {
  Array.from(files).forEach(file => {
    if (file.type.startsWith('image/')) {
      // 检查文件大小（限制为5MB）
      if (file.size > 5 * 1024 * 1024) {
        showNotification('图片大小不能超过5MB', 'error');
        return;
      }
      
      // 读取文件并创建预览
      const reader = new FileReader();
      reader.onload = (e) => {
        addImagePreview(e.target.result);
      };
      reader.readAsDataURL(file);
    } else {
      showNotification('请选择图片文件', 'error');
    }
  });
  
  // 清空文件输入
  document.getElementById('imageFileInput').value = '';
}

// 添加图片预览
function addImagePreview(imageSrc) {
  const container = document.getElementById('imagePreviewContainer');
  
  // 检查是否已达到最大数量（比如6张）
  const existingPreviews = container.querySelectorAll('.image-preview');
  if (existingPreviews.length >= 6) {
    showNotification('最多只能上传6张图片', 'error');
    return;
  }
  
  const previewDiv = document.createElement('div');
  previewDiv.className = 'image-preview';
  previewDiv.innerHTML = `
    <img src="${imageSrc}" alt="图片预览">
    <button type="button" class="image-remove" onclick="removeImagePreview(this)">×</button>
  `;
  
  container.appendChild(previewDiv);
}

// 移除图片预览
function removeImagePreview(button) {
  const previewDiv = button.closest('.image-preview');
  previewDiv.remove();
}

// 取消编辑
function cancelEdit() {
  if (confirm('确定要取消吗？未保存的更改将丢失。')) {
    window.history.back();
  }
}

// 显示通知
function showNotification(message, type = 'info') {
  // 移除现有通知
  document.querySelectorAll('.notification').forEach(n => n.remove());

  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;
  notification.textContent = message;

  document.body.appendChild(notification);

  // 3秒后自动移除
  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, 3000);
}

// 字符计数功能
function updateCharCount() {
  const textarea = document.getElementById('contentTextarea');
  const charCount = document.getElementById('charCount');
  const currentLength = textarea.value.length;
  const maxLength = 2000; // 假设最大长度为2000字符
  
  if (charCount) {
    charCount.textContent = `${currentLength}/${maxLength}`;
    charCount.style.color = currentLength > maxLength ? '#f44336' : '#8f98a0';
  }
}

// 添加字符计数监听器
document.addEventListener('DOMContentLoaded', function() {
  const textarea = document.getElementById('contentTextarea');
  if (textarea) {
    textarea.addEventListener('input', updateCharCount);
    updateCharCount(); // 初始化计数
  }
});