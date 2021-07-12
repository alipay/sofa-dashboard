import React from 'react';
import { Card, Table, Divider, Input, Tag, Button, Tooltip, Badge, Icon, message, Popconfirm } from 'antd';
import { connect } from 'dva';
import RelatedAppForm from './RelatedAppForm';
import RegisterPluginFrom from './RegisterPluginFrom';
import RegisterVersionForm from './RegisterVersionForm';
const { Search } = Input;

@connect(({ ark }) => ({
  list: ark.list,
}))
class Ark extends React.Component {

  state = {
    visible: false,
    confirmLoading: false,
    arkModalData: {},

    relatedAppVisible: false,
    confirmRelatedAppLoading: false,
    pluginRecordId: -1,

    newVersionVisible: false,
    newVersionLoading: false,
  };

  // 获取插件列表
  componentDidMount() {
    const { dispatch } = this.props;
    dispatch({
      type: 'ark/fetchPlugins',
    });
  }
  /**
   * 插件注册相关方法 - 开始
   */

  // 展示对话框
  showModal = () => {
    this.setState({ visible: true });
  };
  // 取消按钮
  handleCancel = () => {
    this.setState({
      visible: false,
      confirmLoading: false,
      arkModalData: {}
    });
  };
  // 提交插件注册或者更新
  handleSubmitRegister = () => {
    const { form } = this.formRef.props;
    const { dispatch } = this.props;
    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      // reset form 
      form.resetFields();
      this.setState({
        confirmLoading: true,
      });
      if (this.state.arkModalData.id === undefined || this.state.arkModalData.id === '') {
        // 创建
        dispatch({
          type: 'ark/pluginRegister',
          payload: {
            "pluginName": values.pluginName,
            "description": values.description
          }
        }).then(data => {
          // 这里有个细节，如果希望注册失败之后，表单数据不被清空，则可以将前面的 form.resetFields() 操作放在这个后面
          if (!data) {
            message.info('注册插件失败，插件名可能已经存在');
          } else {
            this.setState({
              visible: false,
              confirmLoading: false,
            });
          }
        });
      }
      else {
        // 更新
        dispatch({
          type: 'ark/updatePlugin',
          payload: {
            "id": values.id,
            "pluginName": values.pluginName,
            "description": values.description
          }
        }).then(data => {
          if (!data) {
            message.info('插件更新失败，请联系管理员查看');
          }
          this.setState({
            visible: false,
            confirmLoading: false,
          });
        });
      }
    });
  };
  // 用来获取当前表单组件的引用
  saveFormRef = formRef => {
    this.formRef = formRef;
  };

  // 展示关联应用表单
  showRelatedAppModal = (record) => {
    this.setState({
      relatedAppVisible: true,
      pluginRecordId: record.id
    });
  };

  // 取消关联应用提交
  handleRelatedAppCancel = () => {
    this.setState({
      relatedAppVisible: false,
      confirmRelatedAppLoading: false,
    });
  };

  // 执行关联应用提交
  handleRelatedApp = (record) => {
    const { form } = this.formRelatedAppRef.props;
    const { dispatch } = this.props;
    form.validateFields((err, values) => {
      console.log("values"+values)
      if (err) {
        return;
      }
      // reset form 
      form.resetFields();

      this.setState({
        relatedAppVisible: true,
        confirmRelatedAppLoading: true,
      });

      // 关联应用
      dispatch({
        type: 'ark/relatedApp',
        payload: {
          "id": this.state.pluginRecordId,
          "appName": values.appName
        }
      }).then(data => {
        if (data) {
          message.info('关联成功');
          this.setState({
            relatedAppVisible: false,
            confirmRelatedAppLoading: false,
          });
        } else {
          message.info('关联失败，请联系管理员');
          this.setState({
            confirmRelatedAppLoading: false,
          });
        }
      });
    });
  };

  // 用来获取当前表单组件的引用-关联应用
  saveRelatedAppFormRef = formRef => {
    this.formRelatedAppRef = formRef;
  };

  // 确认删除插件记录
  confirm = (record) => {
    const { dispatch } = this.props;
    // 更新
    dispatch({
      type: 'ark/deletePlugin',
      payload: {
        "id": record.id,
      }
    }).then(data => {
      if (data) {
        message.info('删除成功');
      } else {
        message.info('删除失败，请联系管理员');
      }
    });
  };

  // 插件查询
  handleSearch = (value) => {
    const { dispatch } = this.props;
    // 更新
    dispatch({
      type: 'ark/searchPlugins',
      payload: {
        "pluginName": value,
      }
    });
  };

  gotoDetails = (pluginName, record) => {
    window.location.href = `/ark/app?pluginName=` + pluginName + "&appName=" + record.appName;
  }

  cancelRelated = (pluginName, record) => {
    const { dispatch } = this.props;
    // 关联应用
    dispatch({
      type: 'ark/cancelRelatedApp',
      payload: {
        appName: record.appName,
        pluginName: pluginName,
      },
    }).then(data => {
      if (data) {
        message.info('取消关联成功');
      } else {
        message.info('取消关联失败，请联系管理员');
      }

    });
  }

  showPath = (sourcePath) => {
    message.info(sourcePath);
  }

  outMessage = () => {
    message.destroy();
  }

  deleteVersion = (version, id) => {
    const { dispatch } = this.props;
    // 删除版本
    dispatch({
      type: 'ark/deleteVersion',
      payload: {
        version: version,
        id: id,
      },
    }).then(data => {
      if (data.success) {
        message.info('删除版本成功');
      } else {
        message.info(data.error);
      }
    });

  }
  // 展示新增版本界面
  showNewVersionModal = (record) => {
    this.setState({
      newVersionVisible: true,
      pluginRecordId: record.id
    });
  }

  saveNewVersionFormRef = formRef => {
    this.formNewVersionRef = formRef;
  };

  // 取消操作
  handleNewVersionCancel = () => {
    this.setState({
      newVersionVisible: false,
      newVersionLoading: false,
    });
  }

  // 提交新版本
  handleNewVersionSubmit = () => {
    const { form } = this.formNewVersionRef.props;
    const { dispatch } = this.props;
    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      // reset form 
      form.resetFields();
      this.setState({
        newVersionLoading: true,
      });
      // 关联应用
      dispatch({
        type: 'ark/submitVersion',
        payload: {
          "id": this.state.pluginRecordId,
          "version": values.version,
          "address": values.address
        }
      }).then(data => {
        if (data) {
          message.info('新增版本成功');
        } else {
          message.info('新增版本失败，请联系管理员');
        }
        this.setState({
          newVersionVisible: false,
          newVersionLoading: false,
        });

      });
    });
  }

  render() {
    // 嵌套子table
    const expandedRowRender = (parentRecord) => {
      const columns = [
        { title: '应用名', dataIndex: 'appName', key: 'appName' },
        { title: '关联时间', dataIndex: 'createTime', key: 'createTime' },
        {
          title: '实例个数',
          key: 'instanceNum',
          dataIndex: 'instanceNum',
          render: instanceNum => (
            <span>
              <Badge status={instanceNum > 0 ? "success" : "default"} />
              {instanceNum}
            </span>
          ),
        },
        {
          title: 'Action',
          dataIndex: 'operation',
          key: 'operation',
          render: (text, record) => (
            <span className="table-operation">
              <a onClick={e => {
                e.preventDefault();
                this.gotoDetails(parentRecord.pluginName, record);
              }}
              >
                查看详情</a>
              <Divider type="vertical" />

              <Popconfirm
                title="确定取消与当前宿主应用的关联？"
                icon={<Icon type="question-circle-o" style={{ color: 'red' }} />}
                onConfirm={() => this.cancelRelated(parentRecord.pluginName, record)}
              >
                <a href="#"> 取消关联 </a>
              </Popconfirm>
            </span>
          ),
        },
      ];
      return <Table
        columns={columns}
        dataSource={parentRecord.appArkList}
        pagination={false}
        rowKey={item => item.appName}
      />;
    };

    // 表格列定义
    const columns = [
      {
        title: '模块名称',
        dataIndex: 'pluginName',
        key: 'pluginName',
      },
      {
        title: '版本',
        key: 'versions',
        dataIndex: 'versions',
        render: (text, recored) => (
          <span>
            {recored.versions.map((tag, index) => (
              <Popconfirm
                title="Are you sure delete this version？"
                icon={<Icon type="question-circle-o" style={{ color: 'red' }} />}
                onMouseEnter={() => this.showPath(tag.sourcePath)}
                onMouseLeave={() => this.outMessage()}
                onConfirm={() => this.deleteVersion(tag.version, recored.id)}
                okText="Yes"
                key={index}
              >
                <Tag color="blue">
                  {tag.version}
                </Tag>
              </Popconfirm>
            ))}
          </span>
        ),
      },
      {
        title: '模块描述',
        dataIndex: 'description',
        key: 'description',
      },
      {
        title: '操作',
        key: 'action',
        render: (text, record) => (
          <span>
            <a
              onClick={e => {
                e.preventDefault();
                this.showRelatedAppModal(record);
              }}
            >
              关联应用
              </a>
            <Divider type="vertical" />
            <a
              onClick={e => {
                e.preventDefault();
                this.showNewVersionModal(record);
              }}
            >
              添加版本
              </a>
            {/* <Divider type="vertical" />
            <a
              onClick={e => {
                e.preventDefault();
                this.handleDetails(record);
              }}
            >
              详情
              </a> */}
            <Divider type="vertical" />

            <Popconfirm
              title="删除插件，其关联的宿主应用也将被清除！确定删除？"
              icon={<Icon type="question-circle-o" style={{ color: 'red' }} />}
              onConfirm={() => this.confirm(record)}
            >
              <a href="#"> 删除 </a>
            </Popconfirm>
            <Divider type="vertical" />
            <a onClick={() => this.setState({
              visible: true,
              confirmLoading: false,
              arkModalData: record,
            })}>
              修改
              </a>
          </span>
        ),
      },
    ];

    return (
      <div>
        <Card bordered={false} hoverable={false} title="动态模块列表" style={{ marginTop: -15, marginLeft: -15 }}>
          <div>
            <Search placeholder="plugin name" onSearch={value => this.handleSearch(value)} enterButton style={{ width: '30%', height: 30 }} />
            <Button type="primary" onClick={this.showModal} style={{ height: 30, fontSize: 12, marginLeft: 30 }}>
              添加 ARK 插件
                        </Button>
          </div>

          <Table
            dataSource={this.props.list}
            columns={columns}
            style={{ marginTop: 15 }}
            expandedRowRender={record => expandedRowRender(record)}
            rowKey={record => record.pluginName}
          />
        </Card>
        {
          this.state.visible && <RegisterPluginFrom
            arkModalData={this.state.arkModalData}
            wrappedComponentRef={this.saveFormRef}
            visible={this.state.visible}
            confirmLoading={this.state.confirmLoading}
            onCancel={this.handleCancel}
            onCreate={this.handleSubmitRegister}
          />
        },
        {
          this.state.relatedAppVisible && <RelatedAppForm
            wrappedComponentRef={this.saveRelatedAppFormRef}
            visible={this.state.relatedAppVisible}
            confirmLoading={this.state.confirmRelatedAppLoading}
            onCancel={this.handleRelatedAppCancel}
            onCreate={this.handleRelatedApp}
          />
        }
        {
          this.state.newVersionVisible && <RegisterVersionForm
            wrappedComponentRef={this.saveNewVersionFormRef}
            visible={this.state.newVersionVisible}
            confirmLoading={this.state.newVersionLoading}
            onCancel={this.handleNewVersionCancel}
            onCreate={this.handleNewVersionSubmit}
          />
        }

      </div>

    );
  }
};
export default Ark;