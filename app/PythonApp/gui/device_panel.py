from PyQt5.QtWidgets import QGroupBox, QListWidget, QVBoxLayout
class DeviceStatusPanel(QGroupBox):
    def __init__(self, parent=None):
        super().__init__("Devices", parent)
        self.init_ui()
        self.init_placeholder_data()
    def init_ui(self):
        device_layout = QVBoxLayout(self)
        self.device_list = QListWidget()
        device_layout.addWidget(self.device_list)
        self.setMaximumWidth(250)
    def init_placeholder_data(self):
        self.device_list.addItem("Device 1 (Disconnected)")
        self.device_list.addItem("Device 2 (Disconnected)")
    def update_device_status(self, device_index, connected):
        if 0 <= device_index < self.device_list.count():
            item = self.device_list.item(device_index)
            status = "Connected" if connected else "Disconnected"
            item.setText(f"Device {device_index + 1} ({status})")
    def update_all_devices_status(self, connected):
        for i in range(self.device_list.count()):
            self.update_device_status(i, connected)
    def get_device_count(self):
        return self.device_list.count()
    def add_device(self, device_name, connected=False):
        status = "Connected" if connected else "Disconnected"
        self.device_list.addItem(f"{device_name} ({status})")
    def remove_device(self, device_index):
        if 0 <= device_index < self.device_list.count():
            self.device_list.takeItem(device_index)
    def clear_devices(self):
        self.device_list.clear()