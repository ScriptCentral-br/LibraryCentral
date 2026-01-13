--[[
    ORION LIBRARY - VERSÃO BLINDADA (FIX SCROLL & AUTO-SIZE)
    - Scroll: Atualizado para AutomaticCanvasSize (Nativo do Roblox)
    - Performance: Removidos loops de cálculo de tamanho (Mais leve)
    - Estabilidade: Proteção contra erros de HTTP e FileSystem
]]

local UserInputService = game:GetService("UserInputService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local LocalPlayer = game:GetService("Players").LocalPlayer
local Mouse = LocalPlayer:GetMouse()
local HttpService = game:GetService("HttpService")

-- Detecção segura de Parent (Suporte a todos executores)
local PARENT = nil
if gethui then
	PARENT = gethui()
elseif game:GetService("CoreGui") then
	PARENT = game:GetService("CoreGui")
else
	PARENT = LocalPlayer:WaitForChild("PlayerGui")
end

local OrionLib = {
	Elements = {},
	ThemeObjects = {},
	Connections = {},
	Flags = {},
	Themes = {
		Default = {
			Main = Color3.fromRGB(25, 25, 25), -- Mais escuro/Moderno
			Second = Color3.fromRGB(35, 35, 35),
			Stroke = Color3.fromRGB(60, 60, 60),
			Divider = Color3.fromRGB(50, 50, 50),
			Text = Color3.fromRGB(240, 240, 240),
			TextDark = Color3.fromRGB(180, 180, 180),
			Third = Color3.fromRGB(45, 45, 45),      
			Hover = Color3.fromRGB(50, 50, 50),      
			Accent = Color3.fromRGB(0, 122, 204),    
			AccentDark = Color3.fromRGB(0, 90, 158), 
			ToggleOn = Color3.fromRGB(0, 170, 0),  
			ToggleOff = Color3.fromRGB(100, 100, 100),
			Success = Color3.fromRGB(85, 200, 85),   
			Warning = Color3.fromRGB(220, 170, 40),  
			Error = Color3.fromRGB(220, 60, 60)      
		}
	},
	SelectedTheme = "Default",
	Folder = nil,
	SaveCfg = false
}

-- Carregamento de Ícones Seguro
local Icons = {}
task.spawn(function()
	local success, response = pcall(function()
		return HttpService:JSONDecode(game:HttpGetAsync("https://raw.githubusercontent.com/evoincorp/lucideblox/master/src/modules/util/icons.json")).icons
	end)
	if success then Icons = response end
end)

local function GetIcon(IconName)
	if Icons[IconName] then return Icons[IconName] end
	return nil
end

-- Limpeza de Interface Antiga
local Orion = Instance.new("ScreenGui")
Orion.Name = "Orion"
if syn and syn.protect_gui then syn.protect_gui(Orion) end
Orion.Parent = PARENT
Orion.ZIndexBehavior = Enum.ZIndexBehavior.Sibling
Orion.ResetOnSpawn = false

for _, Interface in ipairs(PARENT:GetChildren()) do
	if Interface.Name == Orion.Name and Interface ~= Orion then
		Interface:Destroy()
	end
end

function OrionLib:IsRunning()
	return Orion.Parent == PARENT
end

local function AddConnection(Signal, Function)
	if (not OrionLib:IsRunning()) then return end
	local SignalConnect = Signal:Connect(Function)
	table.insert(OrionLib.Connections, SignalConnect)
	return SignalConnect
end

-- Sistema de Drag Otimizado
local function MakeDraggable(DragPoint, Main)
	local Dragging, DragInput, MousePos, FramePos = false
	AddConnection(DragPoint.InputBegan, function(Input)
		if Input.UserInputType == Enum.UserInputType.MouseButton1 or Input.UserInputType == Enum.UserInputType.Touch then
			Dragging = true
			MousePos = Input.Position
			FramePos = Main.Position

			Input.Changed:Connect(function()
				if Input.UserInputState == Enum.UserInputState.End then
					Dragging = false
				end
			end)
		end
	end)
	AddConnection(DragPoint.InputChanged, function(Input)
		if Input.UserInputType == Enum.UserInputType.MouseMovement or Input.UserInputType == Enum.UserInputType.Touch then
			DragInput = Input
		end
	end)
	AddConnection(UserInputService.InputChanged, function(Input)
		if Input == DragInput and Dragging then
			local Delta = Input.Position - MousePos
			TweenService:Create(Main, TweenInfo.new(0.04, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {
				Position = UDim2.new(FramePos.X.Scale, FramePos.X.Offset + Delta.X, FramePos.Y.Scale, FramePos.Y.Offset + Delta.Y)
			}):Play()
		end
	end)
end

local function Create(Name, Properties, Children)
	local Object = Instance.new(Name)
	for i, v in next, Properties or {} do
		Object[i] = v
	end
	for i, v in next, Children or {} do
		v.Parent = Object
	end
	return Object
end

local function CreateElement(ElementName, ElementFunction)
	OrionLib.Elements[ElementName] = function(...)
		return ElementFunction(...)
	end
end

local function AddItemTable(Table, Item, Value)
	local Item = tostring(Item)
	local Count = 1
	while Table[Item] do
		Count = Count + 1
		Item = string.format('%s-%d', Item, Count)
	end
	Table[Item] = Value
end

local function MakeElement(ElementName, ...)
	return OrionLib.Elements[ElementName](...)
end

local function SetProps(Element, Props)
	for Property, Value in pairs(Props) do
		Element[Property] = Value
	end
	return Element
end

local function SetChildren(Element, Children)
	for _, Child in pairs(Children) do
		Child.Parent = Element
	end
	return Element
end

local function ReturnProperty(Object)
	if Object:IsA("Frame") or Object:IsA("TextButton") then return "BackgroundColor3" end
	if Object:IsA("ScrollingFrame") then return "ScrollBarImageColor3" end
	if Object:IsA("UIStroke") then return "Color" end
	if Object:IsA("TextLabel") or Object:IsA("TextBox") then return "TextColor3" end
	if Object:IsA("ImageLabel") or Object:IsA("ImageButton") then return "ImageColor3" end
end

local function AddThemeObject(Object, Type)
	if not OrionLib.ThemeObjects[Type] then OrionLib.ThemeObjects[Type] = {} end
	table.insert(OrionLib.ThemeObjects[Type], Object)
	local themeColor = OrionLib.Themes[OrionLib.SelectedTheme][Type]
	if themeColor then Object[ReturnProperty(Object)] = themeColor end
	return Object
end

local function PackColor(Color)
	return {R = Color.R * 255, G = Color.G * 255, B = Color.B * 255}
end

local function UnpackColor(Color)
	return Color3.fromRGB(Color.R, Color.G, Color.B)
end

local function LoadCfg(Config)
	local Data = HttpService:JSONDecode(Config)
	for a, b in pairs(Data) do
		if OrionLib.Flags[a] then
			spawn(function()
				if OrionLib.Flags[a].Type == "Colorpicker" then
					OrionLib.Flags[a]:Set(UnpackColor(b))
				else
					OrionLib.Flags[a]:Set(b)
				end
			end)
		end
	end
end

local function SaveCfg(Name)
	if not OrionLib.SaveCfg then return end
	local Data = {}
	for i, v in pairs(OrionLib.Flags) do
		if v.Save then
			if v.Type == "Colorpicker" then
				Data[i] = PackColor(v.Value)
			else
				Data[i] = v.Value
			end
		end
	end
	if writefile then
		pcall(function() writefile(OrionLib.Folder .. "/" .. Name .. ".txt", tostring(HttpService:JSONEncode(Data))) end)
	end
end

-- ELEMENTOS UI BÁSICOS --

CreateElement("Corner", function(Scale, Offset)
	return Create("UICorner", { CornerRadius = UDim.new(Scale or 0, Offset or 8) })
end)

CreateElement("Stroke", function(Color, Thickness, Transparency)
	return Create("UIStroke", {
		Color = Color or Color3.fromRGB(255, 255, 255),
		Thickness = Thickness or 1,
		Transparency = Transparency or 0
	})
end)

CreateElement("List", function(Scale, Offset)
	return Create("UIListLayout", {
		SortOrder = Enum.SortOrder.LayoutOrder,
		Padding = UDim.new(Scale or 0, Offset or 6)
	})
end)

CreateElement("Padding", function(Bottom, Left, Right, Top)
	return Create("UIPadding", {
		PaddingBottom = UDim.new(0, Bottom or 4),
		PaddingLeft = UDim.new(0, Left or 4),
		PaddingRight = UDim.new(0, Right or 4),
		PaddingTop = UDim.new(0, Top or 4)
	})
end)

CreateElement("TFrame", function()
	return Create("Frame", { BackgroundTransparency = 1 })
end)

CreateElement("Frame", function(Color)
	return Create("Frame", {
		BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255),
		BorderSizePixel = 0
	})
end)

CreateElement("RoundFrame", function(Color, Scale, Offset)
	return Create("Frame", {
		BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255),
		BorderSizePixel = 0
	}, {
		Create("UICorner", { CornerRadius = UDim.new(Scale or 0, Offset or 8) })
	})
end)

CreateElement("Button", function()
	return Create("TextButton", {
		Text = "",
		AutoButtonColor = false,
		BackgroundTransparency = 1,
		BorderSizePixel = 0
	})
end)

-- *** CORREÇÃO CRÍTICA DO SCROLL ***
CreateElement("ScrollFrame", function(Color, Width)
	return Create("ScrollingFrame", {
		BackgroundTransparency = 1,
		MidImage = "rbxassetid://7445543667",
		BottomImage = "rbxassetid://7445543667",
		TopImage = "rbxassetid://7445543667",
		ScrollBarImageColor3 = Color,
		BorderSizePixel = 0,
		ScrollBarThickness = Width,
		
		-- CONFIGURAÇÃO PONTA A PONTA PARA FUNCIONAR O SCROLL
		CanvasSize = UDim2.new(0, 0, 0, 0), -- Deixe 0, o Automatic cuida do resto
		AutomaticCanvasSize = Enum.AutomaticSize.Y, -- Isso faz o scroll funcionar sozinho
		ScrollingDirection = Enum.ScrollingDirection.Y,
		ElasticBehavior = Enum.ElasticBehavior.WhenScrollable
	})
end)

CreateElement("Image", function(ImageID)
	local ImageNew = Create("ImageLabel", {
		Image = ImageID,
		BackgroundTransparency = 1
	})
	if GetIcon(ImageID) then ImageNew.Image = GetIcon(ImageID) end
	return ImageNew
end)

CreateElement("ImageButton", function(ImageID)
	return Create("ImageButton", {
		Image = ImageID,
		BackgroundTransparency = 1
	})
end)

CreateElement("Label", function(Text, TextSize, Transparency)
	return Create("TextLabel", {
		Text = Text or "",
		TextColor3 = Color3.fromRGB(240, 240, 240),
		TextTransparency = Transparency or 0,
		TextSize = TextSize or 15,
		Font = Enum.Font.GothamMedium,
		RichText = true,
		BackgroundTransparency = 1,
		TextXAlignment = Enum.TextXAlignment.Left
	})
end)

local NotificationHolder = SetProps(SetChildren(MakeElement("TFrame"), {
	SetProps(MakeElement("List"), {
		HorizontalAlignment = Enum.HorizontalAlignment.Center,
		SortOrder = Enum.SortOrder.LayoutOrder,
		VerticalAlignment = Enum.VerticalAlignment.Bottom,
		Padding = UDim.new(0, 10)
	})
}), {
	Position = UDim2.new(1, -25, 1, -25),
	Size = UDim2.new(0, 300, 1, -25),
	AnchorPoint = Vector2.new(1, 1),
	Parent = Orion
})

function OrionLib:MakeNotification(NotificationConfig)
	spawn(function()
		NotificationConfig.Name = NotificationConfig.Name or "Notification"
		NotificationConfig.Content = NotificationConfig.Content or "Test"
		NotificationConfig.Image = NotificationConfig.Image or "rbxassetid://103928780885515"
		NotificationConfig.Time = NotificationConfig.Time or 5

		local NotificationParent = SetProps(MakeElement("TFrame"), {
			Size = UDim2.new(1, 0, 0, 0),
			AutomaticSize = Enum.AutomaticSize.Y,
			Parent = NotificationHolder
		})

		local NotificationFrame = SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(25, 25, 25), 0, 10), {
			Parent = NotificationParent,
			Size = UDim2.new(1, 0, 0, 0),
			Position = UDim2.new(1, 50, 0, 0),
			BackgroundTransparency = 0.1,
			AutomaticSize = Enum.AutomaticSize.Y
		}), {
			MakeElement("Stroke", Color3.fromRGB(60, 60, 60), 1, 0.5),
			MakeElement("Padding", 12, 12, 12, 12),
			SetProps(MakeElement("Image", NotificationConfig.Image), {
				Size = UDim2.new(0, 24, 0, 24),
				ImageColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Accent,
			}),
			SetProps(MakeElement("Label", NotificationConfig.Name, 16), {
				Size = UDim2.new(1, -34, 0, 24),
				Position = UDim2.new(0, 34, 0, 0),
				Font = Enum.Font.GothamBold,
				TextColor3 = Color3.fromRGB(255, 255, 255)
			}),
			SetProps(MakeElement("Label", NotificationConfig.Content, 14), {
				Size = UDim2.new(1, 0, 0, 0),
				Position = UDim2.new(0, 0, 0, 30),
				Font = Enum.Font.Gotham,
				RichText = true,
				AutomaticSize = Enum.AutomaticSize.Y,
				TextColor3 = Color3.fromRGB(200, 200, 200),
				TextWrapped = true
			})
		})

		TweenService:Create(NotificationFrame, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Position = UDim2.new(0, 0, 0, 0)}):Play()

		wait(NotificationConfig.Time)
		
		TweenService:Create(NotificationFrame, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.In), {Position = UDim2.new(1, 50, 0, 0)}):Play()
		wait(0.5)
		NotificationFrame:Destroy()
		NotificationParent:Destroy()
	end)
end

function OrionLib:MakeWindow(WindowConfig)
	WindowConfig = WindowConfig or {}
	WindowConfig.Name = WindowConfig.Name or "ScriptCentral"
	WindowConfig.ConfigFolder = WindowConfig.ConfigFolder or "OrionConfig"
	WindowConfig.SaveConfig = WindowConfig.SaveConfig or false
	
	OrionLib.Folder = WindowConfig.ConfigFolder
	OrionLib.SaveCfg = WindowConfig.SaveConfig

	if WindowConfig.SaveConfig and makefolder and not isfolder(WindowConfig.ConfigFolder) then
		pcall(function() makefolder(WindowConfig.ConfigFolder) end)
	end

	local TabHolder = AddThemeObject(SetChildren(SetProps(MakeElement("ScrollFrame", Color3.fromRGB(255, 255, 255), 4), {
		Size = UDim2.new(1, 0, 1, -50),
		CanvasSize = UDim2.new(0, 0, 0, 0),
		AutomaticCanvasSize = Enum.AutomaticSize.Y -- FIX SCROLL ABAS
	}), {
		MakeElement("List"),
		MakeElement("Padding", 8, 0, 0, 8)
	}), "Divider")

	local DragPoint = SetProps(MakeElement("TFrame"), {
		Size = UDim2.new(1, 0, 0, 50)
	})

	local MainWindow = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 12), {
		Parent = Orion,
		Position = UDim2.new(0.5, 0, 0.5, 0),
		AnchorPoint = Vector2.new(0.5, 0.5), 
		Size = UDim2.new(0, 0, 0, 0), 
		ClipsDescendants = true,
		Visible = false 
	}), {
		SetChildren(SetProps(MakeElement("TFrame"), {
			Size = UDim2.new(1, 0, 0, 50),
			Name = "TopBar"
		}), {
			AddThemeObject(SetProps(MakeElement("Label", WindowConfig.Name, 18), {
				Size = UDim2.new(1, -50, 1, 0),
				Position = UDim2.new(0, 25, 0, 0),
				Font = Enum.Font.GothamBold,
				TextXAlignment = Enum.TextXAlignment.Left
			}), "Text"),
			AddThemeObject(SetProps(MakeElement("Frame"), { 
				Size = UDim2.new(1, 0, 0, 1),
				Position = UDim2.new(0, 0, 1, -1)
			}), "Divider"),
		}),
		DragPoint,
		AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 12), {
			Size = UDim2.new(0, 170, 1, -50),
			Position = UDim2.new(0, 0, 0, 50)
		}), {
			AddThemeObject(SetProps(MakeElement("Frame"), {
				Size = UDim2.new(1, 0, 0, 1),
				Position = UDim2.new(0, 0, 0, 0)
			}), "Second"),
			AddThemeObject(SetProps(MakeElement("Frame"), {
				Size = UDim2.new(0, 1, 1, 0),
				Position = UDim2.new(1, -1, 0, 0)
			}), "Divider"),
			TabHolder
		}), "Second"),
		AddThemeObject(SetProps(MakeElement("Stroke", Color3.new(0,0,0), 3, 0.7),{}),"Stroke") 
	}), "Main")

	MakeDraggable(DragPoint, MainWindow)
	
	MainWindow.Visible = true
	TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {
		Size = UDim2.new(0, 650, 0, 380)
	}):Play()

	local Tabs = {}
	local FirstTab = true

	local Functions = {}

	function Functions:MakeTab(TabConfig)
		TabConfig = TabConfig or {}
		TabConfig.Name = TabConfig.Name or "Tab"
		
		local TabFrame = SetChildren(SetProps(MakeElement("Button"), {
			Size = UDim2.new(1, -12, 0, 36),
			Parent = TabHolder
		}), {
			MakeElement("Corner", 0, 6),
			AddThemeObject(SetProps(MakeElement("Image", TabConfig.Icon), {
				AnchorPoint = Vector2.new(0, 0.5),
				Size = UDim2.new(0, 20, 0, 20),
				Position = UDim2.new(0, 10, 0.5, 0),
				ImageTransparency = 0.5,
				Name = "Ico"
			}), "Text"),
			AddThemeObject(SetProps(MakeElement("Label", TabConfig.Name, 15), {
				Size = UDim2.new(1, -35, 1, 0),
				Position = UDim2.new(0, 40, 0, 0),
				Font = Enum.Font.GothamMedium,
				TextTransparency = 0.5,
				Name = "Title"
			}), "Text")
		})

		-- CONTAINER PRINCIPAL DA TAB (Onde fica o scroll)
		local Container = AddThemeObject(SetChildren(SetProps(MakeElement("ScrollFrame", Color3.fromRGB(255, 255, 255), 5), {
			Size = UDim2.new(1, -170, 1, -50),
			Position = UDim2.new(0, 170, 0, 50),
			Parent = MainWindow,
			Visible = false,
			Name = "ItemContainer",
			BackgroundTransparency = 1,
			
			-- FIX SCROLL ITENS (Fundamental)
			CanvasSize = UDim2.new(0, 0, 0, 0),
			AutomaticCanvasSize = Enum.AutomaticSize.Y
		}), {
			MakeElement("List", 0, 8),
			MakeElement("Padding", 15, 15, 15, 15)
		}), "Divider")

		if FirstTab then
			FirstTab = false
			TabFrame.BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Second
			TabFrame.BackgroundTransparency = 0
			TabFrame.Title.TextTransparency = 0
			TabFrame.Ico.ImageTransparency = 0
			Container.Visible = true
		end

		TabFrame.MouseButton1Click:Connect(function()
			for _, Tab in next, TabHolder:GetChildren() do
				if Tab:IsA("TextButton") then
					TweenService:Create(Tab, TweenInfo.new(0.3), {BackgroundTransparency = 1}):Play()
					TweenService:Create(Tab.Title, TweenInfo.new(0.3), {TextTransparency = 0.5}):Play()
					TweenService:Create(Tab.Ico, TweenInfo.new(0.3), {ImageTransparency = 0.5}):Play()
				end
			end
			for _, Item in next, MainWindow:GetChildren() do
				if Item.Name == "ItemContainer" then Item.Visible = false end
			end
			
			TweenService:Create(TabFrame, TweenInfo.new(0.3), {BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Second, BackgroundTransparency = 0}):Play()
			TweenService:Create(TabFrame.Title, TweenInfo.new(0.3), {TextTransparency = 0}):Play()
			TweenService:Create(TabFrame.Ico, TweenInfo.new(0.3), {ImageTransparency = 0}):Play()
			
			Container.Visible = true
		end)

		local function GetElements(ItemParent)
			local ElementFunction = {}

			function ElementFunction:AddSection(SectionConfig)
				local SectionFrame = SetChildren(SetProps(MakeElement("TFrame"), {
					Size = UDim2.new(1, 0, 0, 0),
					AutomaticSize = Enum.AutomaticSize.Y, -- FIX SEÇÃO CRESCER SOZINHA
					Parent = Container
				}), {
					AddThemeObject(SetProps(MakeElement("Label", SectionConfig.Name, 13), {
						Size = UDim2.new(1, -12, 0, 24),
						Position = UDim2.new(0, 5, 0, 0),
						Font = Enum.Font.GothamBold,
						TextColor3 = OrionLib.Themes[OrionLib.SelectedTheme].TextDark
					}), "TextDark"),
					SetChildren(SetProps(MakeElement("TFrame"), {
						Size = UDim2.new(1, 0, 0, 0),
						AutomaticSize = Enum.AutomaticSize.Y, -- FIX HOLDER CRESCER SOZINHO
						Position = UDim2.new(0, 0, 0, 24),
						Name = "Holder"
					}), { MakeElement("List", 0, 8) }),
				})

				local SectionFunction = {}
				for i, v in next, GetElements(SectionFrame.Holder) do SectionFunction[i] = v end
				return SectionFunction
			end

			function ElementFunction:AddButton(ButtonConfig)
				ButtonConfig = ButtonConfig or {}
				ButtonConfig.Name = ButtonConfig.Name or "Button"
				ButtonConfig.Callback = ButtonConfig.Callback or function() end

				local ButtonFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), {
					Size = UDim2.new(1, 0, 0, 36),
					Parent = ItemParent
				}), {
					AddThemeObject(SetProps(MakeElement("Label", ButtonConfig.Name, 14), {
						Size = UDim2.new(1, -40, 1, 0),
						Position = UDim2.new(0, 12, 0, 0),
						Font = Enum.Font.GothamBold,
						Name = "Content"
					}), "Text"),
					AddThemeObject(SetProps(MakeElement("Image", "rbxassetid://3944703587"), {
						Size = UDim2.new(0, 20, 0, 20),
						Position = UDim2.new(1, -30, 0, 8),
					}), "TextDark"),
					AddThemeObject(MakeElement("Stroke"), "Stroke"),
					SetProps(MakeElement("Button"), {Size = UDim2.new(1, 0, 1, 0)})
				}), "Second")

				local Click = ButtonFrame.TextButton
				Click.MouseButton1Click:Connect(function()
					spawn(function() ButtonConfig.Callback() end)
					TweenService:Create(ButtonFrame, TweenInfo.new(0.1), {Size = UDim2.new(1, -4, 0, 32)}):Play()
					wait(0.1)
					TweenService:Create(ButtonFrame, TweenInfo.new(0.1), {Size = UDim2.new(1, 0, 0, 36)}):Play()
				end)
				
				Click.MouseEnter:Connect(function()
					TweenService:Create(ButtonFrame, TweenInfo.new(0.2), {BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Hover}):Play()
				end)
				Click.MouseLeave:Connect(function()
					TweenService:Create(ButtonFrame, TweenInfo.new(0.2), {BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Second}):Play()
				end)
			end

			function ElementFunction:AddParagraph(Title, Content)
				local ParagraphFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), {
					Size = UDim2.new(1, 0, 0, 0),
					AutomaticSize = Enum.AutomaticSize.Y,
					Parent = ItemParent
				}), {
					AddThemeObject(SetProps(MakeElement("Label", Title, 15), {
						Size = UDim2.new(1, -24, 0, 16),
						Position = UDim2.new(0, 12, 0, 12),
						Font = Enum.Font.GothamBold
					}), "Text"),
					AddThemeObject(SetProps(MakeElement("Label", Content, 13), {
						Size = UDim2.new(1, -24, 0, 0),
						Position = UDim2.new(0, 12, 0, 32),
						Font = Enum.Font.Gotham,
						TextWrapped = true,
						AutomaticSize = Enum.AutomaticSize.Y
					}), "TextDark"),
					AddThemeObject(MakeElement("Stroke"), "Stroke"),
					MakeElement("Padding", 12, 0, 0, 0)
				}), "Second")
			end
			
			function ElementFunction:AddDropdown(DropdownConfig)
				-- (Simplificado para estabilidade, adicione se precisar da versão full)
				-- Dropdowns complexos costumam travar auto-load
			end

			return ElementFunction
		end

		for i, v in next, GetElements(Container) do ElementFunction[i] = v end
		return ElementFunction
	end
	
	function OrionLib:Destroy()
		Orion:Destroy()
	end

	return Functions
end

return OrionLib
