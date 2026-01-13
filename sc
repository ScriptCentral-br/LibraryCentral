--[[
    ORION LIBRARY - VERSÃO DEFINITIVA (FIXED & FULL)
    - Scroll: AutomaticCanvasSize (Nativo e liso)
    - Completa: Todas as funções restauradas (Toggle, Slider, Dropdown, etc.)
    - Estável: Proteção contra erros e suporte a todos os executores.
]]

local UserInputService = game:GetService("UserInputService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local LocalPlayer = game:GetService("Players").LocalPlayer
local Mouse = LocalPlayer:GetMouse()
local HttpService = game:GetService("HttpService")

-- Detecção segura de Parent
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
			Main = Color3.fromRGB(25, 25, 25),
			Second = Color3.fromRGB(35, 35, 35),
			Stroke = Color3.fromRGB(60, 60, 60),
			Divider = Color3.fromRGB(50, 50, 50),
			Text = Color3.fromRGB(240, 240, 240),
			TextDark = Color3.fromRGB(180, 180, 180),
			Third = Color3.fromRGB(45, 45, 45),      
			Hover = Color3.fromRGB(45, 45, 45),      
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

-- Ícones
local Icons = {}
task.spawn(function()
	local s, r = pcall(function()
		return HttpService:JSONDecode(game:HttpGetAsync("https://raw.githubusercontent.com/evoincorp/lucideblox/master/src/modules/util/icons.json")).icons
	end)
	if s then Icons = r end
end)

local function GetIcon(IconName)
	if Icons[IconName] then return Icons[IconName] end
	return nil
end

-- Limpeza
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

local function MakeDraggable(DragPoint, Main)
	local Dragging, DragInput, MousePos, FramePos = false
	AddConnection(DragPoint.InputBegan, function(Input)
		if Input.UserInputType == Enum.UserInputType.MouseButton1 or Input.UserInputType == Enum.UserInputType.Touch then
			Dragging = true
			MousePos = Input.Position
			FramePos = Main.Position
			Input.Changed:Connect(function()
				if Input.UserInputState == Enum.UserInputState.End then Dragging = false end
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
	for i, v in next, Properties or {} do Object[i] = v end
	for i, v in next, Children or {} do v.Parent = Object end
	return Object
end

local function CreateElement(ElementName, ElementFunction)
	OrionLib.Elements[ElementName] = function(...) return ElementFunction(...) end
end

local function MakeElement(ElementName, ...)
	return OrionLib.Elements[ElementName](...)
end

local function SetProps(Element, Props)
	for Property, Value in pairs(Props) do Element[Property] = Value end
	return Element
end

local function SetChildren(Element, Children)
	for _, Child in pairs(Children) do Child.Parent = Element end
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

local function SaveCfg(Name)
	if not OrionLib.SaveCfg then return end
	local Data = {}
	for i, v in pairs(OrionLib.Flags) do
		if v.Save then
			if v.Type == "Colorpicker" then Data[i] = PackColor(v.Value) else Data[i] = v.Value end
		end
	end
	if writefile then pcall(function() writefile(OrionLib.Folder .. "/" .. Name .. ".txt", tostring(HttpService:JSONEncode(Data))) end) end
end

-- ELEMENTOS BÁSICOS
CreateElement("Corner", function(Scale, Offset) return Create("UICorner", { CornerRadius = UDim.new(Scale or 0, Offset or 8) }) end)
CreateElement("Stroke", function(Color, Thickness, Transparency) return Create("UIStroke", { Color = Color or Color3.fromRGB(255, 255, 255), Thickness = Thickness or 1, Transparency = Transparency or 0 }) end)
CreateElement("List", function(Scale, Offset) return Create("UIListLayout", { SortOrder = Enum.SortOrder.LayoutOrder, Padding = UDim.new(Scale or 0, Offset or 6) }) end)
CreateElement("Padding", function(Bottom, Left, Right, Top) return Create("UIPadding", { PaddingBottom = UDim.new(0, Bottom or 4), PaddingLeft = UDim.new(0, Left or 4), PaddingRight = UDim.new(0, Right or 4), PaddingTop = UDim.new(0, Top or 4) }) end)
CreateElement("TFrame", function() return Create("Frame", { BackgroundTransparency = 1 }) end)
CreateElement("Frame", function(Color) return Create("Frame", { BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255), BorderSizePixel = 0 }) end)
CreateElement("RoundFrame", function(Color, Scale, Offset) return Create("Frame", { BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255), BorderSizePixel = 0 }, { Create("UICorner", { CornerRadius = UDim.new(Scale or 0, Offset or 8) }) }) end)
CreateElement("Button", function() return Create("TextButton", { Text = "", AutoButtonColor = false, BackgroundTransparency = 1, BorderSizePixel = 0 }) end)

-- *** FIX SCROLL ***
CreateElement("ScrollFrame", function(Color, Width)
	return Create("ScrollingFrame", {
		BackgroundTransparency = 1,
		ScrollBarImageColor3 = Color,
		BorderSizePixel = 0,
		ScrollBarThickness = Width,
		CanvasSize = UDim2.new(0, 0, 0, 0),
		AutomaticCanvasSize = Enum.AutomaticSize.Y, -- O Segredo do Scroll infinito
		ScrollingDirection = Enum.ScrollingDirection.Y,
		ElasticBehavior = Enum.ElasticBehavior.WhenScrollable
	})
end)

CreateElement("Image", function(ImageID)
	local ImageNew = Create("ImageLabel", { Image = ImageID, BackgroundTransparency = 1 })
	if GetIcon(ImageID) then ImageNew.Image = GetIcon(ImageID) end
	return ImageNew
end)
CreateElement("ImageButton", function(ImageID) return Create("ImageButton", { Image = ImageID, BackgroundTransparency = 1 }) end)
CreateElement("Label", function(Text, TextSize, Transparency) return Create("TextLabel", { Text = Text or "", TextColor3 = Color3.fromRGB(240, 240, 240), TextTransparency = Transparency or 0, TextSize = TextSize or 15, Font = Enum.Font.GothamMedium, RichText = true, BackgroundTransparency = 1, TextXAlignment = Enum.TextXAlignment.Left }) end)

-- NOTIFICAÇÕES
local NotificationHolder = SetProps(SetChildren(MakeElement("TFrame"), { SetProps(MakeElement("List"), { HorizontalAlignment = Enum.HorizontalAlignment.Center, SortOrder = Enum.SortOrder.LayoutOrder, VerticalAlignment = Enum.VerticalAlignment.Bottom, Padding = UDim.new(0, 10) }) }), { Position = UDim2.new(1, -25, 1, -25), Size = UDim2.new(0, 300, 1, -25), AnchorPoint = Vector2.new(1, 1), Parent = Orion })
function OrionLib:MakeNotification(Config)
	spawn(function()
		Config.Name = Config.Name or "Notification"
		Config.Content = Config.Content or "Test"
		Config.Time = Config.Time or 5
		local NotifFrame = SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(25, 25, 25), 0, 10), { Parent = NotificationHolder, Size = UDim2.new(1, 0, 0, 0), AutomaticSize = Enum.AutomaticSize.Y, BackgroundTransparency = 0.1 }), { MakeElement("Stroke", Color3.fromRGB(60,60,60), 1), MakeElement("Padding", 12, 12, 12, 12), SetProps(MakeElement("Label", Config.Name, 16), { Font = Enum.Font.GothamBold }), SetProps(MakeElement("Label", Config.Content, 14), { Position = UDim2.new(0,0,0,25), TextColor3 = Color3.fromRGB(200,200,200), AutomaticSize = Enum.AutomaticSize.Y, TextWrapped = true }) })
		wait(Config.Time)
		NotifFrame:Destroy()
	end)
end

function OrionLib:MakeWindow(WindowConfig)
	WindowConfig = WindowConfig or {}
	WindowConfig.Name = WindowConfig.Name or "ScriptCentral"
	WindowConfig.ConfigFolder = WindowConfig.ConfigFolder or "OrionConfig"
	WindowConfig.SaveConfig = WindowConfig.SaveConfig or false
	WindowConfig.IntroEnabled = WindowConfig.IntroEnabled == nil and true or WindowConfig.IntroEnabled
	
	OrionLib.Folder = WindowConfig.ConfigFolder
	OrionLib.SaveCfg = WindowConfig.SaveConfig
	if WindowConfig.SaveConfig and makefolder and not isfolder(WindowConfig.ConfigFolder) then pcall(function() makefolder(WindowConfig.ConfigFolder) end) end

	local TabHolder = AddThemeObject(SetChildren(SetProps(MakeElement("ScrollFrame", Color3.fromRGB(255, 255, 255), 4), { Size = UDim2.new(1, 0, 1, -50), CanvasSize = UDim2.new(0, 0, 0, 0), AutomaticCanvasSize = Enum.AutomaticSize.Y }), { MakeElement("List"), MakeElement("Padding", 8, 0, 0, 8) }), "Divider")
	local DragPoint = SetProps(MakeElement("TFrame"), { Size = UDim2.new(1, 0, 0, 50) })
	
	local MainWindow = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(25, 25, 25), 0, 12), { Parent = Orion, Position = UDim2.new(0.5, 0, 0.5, 0), AnchorPoint = Vector2.new(0.5, 0.5), Size = UDim2.new(0, 0, 0, 0), ClipsDescendants = true, Visible = false }), {
		SetChildren(SetProps(MakeElement("TFrame"), { Size = UDim2.new(1, 0, 0, 50), Name = "TopBar" }), {
			AddThemeObject(SetProps(MakeElement("Label", WindowConfig.Name, 18), { Size = UDim2.new(1, -100, 1, 0), Position = UDim2.new(0, 25, 0, 0), Font = Enum.Font.GothamBold }), "Text"),
			AddThemeObject(SetProps(MakeElement("Frame"), { Size = UDim2.new(1, 0, 0, 1), Position = UDim2.new(0, 0, 1, -1) }), "Divider")
		}),
		DragPoint,
		AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 12), { Size = UDim2.new(0, 170, 1, -50), Position = UDim2.new(0, 0, 0, 50) }), {
			AddThemeObject(SetProps(MakeElement("Frame"), { Size = UDim2.new(0, 1, 1, 0), Position = UDim2.new(1, -1, 0, 0) }), "Divider"), TabHolder
		}), "Second"),
		AddThemeObject(SetProps(MakeElement("Stroke", Color3.new(0,0,0), 3, 0.7),{}),"Stroke") 
	}), "Main")
	
	MakeDraggable(DragPoint, MainWindow)

	-- BOTOES DA JANELA
	local CloseBtn = SetChildren(SetProps(MakeElement("Button"), { Parent = MainWindow.TopBar, Size = UDim2.new(0, 30, 0, 30), Position = UDim2.new(1, -35, 0, 10) }), { SetProps(MakeElement("Image", "rbxassetid://7072725342"), { Size = UDim2.new(0, 16, 0, 16), Position = UDim2.new(0, 7, 0, 7), ImageColor3 = Color3.fromRGB(200, 200, 200) }) })
	local MinBtn = SetChildren(SetProps(MakeElement("Button"), { Parent = MainWindow.TopBar, Size = UDim2.new(0, 30, 0, 30), Position = UDim2.new(1, -70, 0, 10) }), { SetProps(MakeElement("Image", "rbxassetid://7072719338"), { Size = UDim2.new(0, 16, 0, 16), Position = UDim2.new(0, 7, 0, 7), ImageColor3 = Color3.fromRGB(200, 200, 200) }) })

	-- Lógica de Abrir/Fechar
	local OpenBtn = SetChildren(SetProps(MakeElement("ImageButton", "rbxassetid://103928780885515"), { Parent = Orion, Size = UDim2.new(0, 45, 0, 45), Position = UDim2.new(0, 10, 0.5, 0), Visible = false, BackgroundColor3 = Color3.fromRGB(25,25,25), BackgroundTransparency = 0.2 }), { MakeElement("Corner", 0, 12), MakeElement("Stroke", Color3.fromRGB(60,60,60)) })
	MakeDraggable(OpenBtn, OpenBtn)

	CloseBtn.MouseButton1Click:Connect(function() MainWindow.Visible = false OpenBtn.Visible = true end)
	OpenBtn.MouseButton1Click:Connect(function() MainWindow.Visible = true OpenBtn.Visible = false end)
	
	local Minimized = false
	MinBtn.MouseButton1Click:Connect(function()
		Minimized = not Minimized
		if Minimized then
			TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 50)}):Play()
		else
			TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
		end
	end)

	-- Intro
	if WindowConfig.IntroEnabled then
		spawn(function()
			local Intro = SetChildren(SetProps(MakeElement("Frame", Color3.fromRGB(25,25,25)), { Parent = Orion, Size = UDim2.new(1,0,1,0), ZIndex = 999 }), { SetProps(MakeElement("Label", WindowConfig.Name, 20), { Position = UDim2.new(0.5,0,0.5,0), AnchorPoint = Vector2.new(0.5,0.5) }) })
			wait(1)
			TweenService:Create(Intro, TweenInfo.new(0.5), {BackgroundTransparency = 1}):Play()
			TweenService:Create(Intro.TextLabel, TweenInfo.new(0.5), {TextTransparency = 1}):Play()
			wait(0.5) Intro:Destroy()
			MainWindow.Visible = true
			TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
		end)
	else
		MainWindow.Visible = true
		TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
	end

	local Tabs = {}
	local FirstTab = true
	local Functions = {}

	function Functions:MakeTab(Config)
		Config.Name = Config.Name or "Tab"
		local TabBtn = SetChildren(SetProps(MakeElement("Button"), { Parent = TabHolder, Size = UDim2.new(1, -12, 0, 36) }), {
			MakeElement("Corner", 0, 6),
			AddThemeObject(SetProps(MakeElement("Image", Config.Icon), { Size = UDim2.new(0, 20, 0, 20), Position = UDim2.new(0, 10, 0.5, 0), AnchorPoint = Vector2.new(0, 0.5), ImageTransparency = 0.5 }), "Text"),
			AddThemeObject(SetProps(MakeElement("Label", Config.Name, 15), { Position = UDim2.new(0, 40, 0, 0), Size = UDim2.new(1, -40, 1, 0), TextTransparency = 0.5 }), "Text")
		})

		local Container = AddThemeObject(SetChildren(SetProps(MakeElement("ScrollFrame", Color3.fromRGB(255, 255, 255), 5), { Parent = MainWindow, Size = UDim2.new(1, -170, 1, -50), Position = UDim2.new(0, 170, 0, 50), Visible = false, AutomaticCanvasSize = Enum.AutomaticSize.Y }), { MakeElement("List", 0, 8), MakeElement("Padding", 15, 15, 15, 15) }), "Divider")

		if FirstTab then
			FirstTab = false
			TabBtn.BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Second
			TabBtn.BackgroundTransparency = 0
			TabBtn.TextLabel.TextTransparency = 0
			TabBtn.ImageLabel.ImageTransparency = 0
			Container.Visible = true
		end

		TabBtn.MouseButton1Click:Connect(function()
			for _, t in pairs(TabHolder:GetChildren()) do if t:IsA("TextButton") then TweenService:Create(t, TweenInfo.new(0.3), {BackgroundTransparency = 1}):Play() TweenService:Create(t.TextLabel, TweenInfo.new(0.3), {TextTransparency = 0.5}):Play() TweenService:Create(t.ImageLabel, TweenInfo.new(0.3), {ImageTransparency = 0.5}):Play() end end
			for _, c in pairs(MainWindow:GetChildren()) do if c.Name == "ScrollingFrame" and c ~= TabHolder then c.Visible = false end end
			TweenService:Create(TabBtn, TweenInfo.new(0.3), {BackgroundColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Second, BackgroundTransparency = 0}):Play()
			TweenService:Create(TabBtn.TextLabel, TweenInfo.new(0.3), {TextTransparency = 0}):Play()
			TweenService:Create(TabBtn.ImageLabel, TweenInfo.new(0.3), {ImageTransparency = 0}):Play()
			Container.Visible = true
		end)

		local Elements = {}
		function Elements:AddSection(Config)
			local Section = SetChildren(SetProps(MakeElement("TFrame"), { Parent = Container, AutomaticSize = Enum.AutomaticSize.Y }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 13), { Size = UDim2.new(1, -10, 0, 24), Position = UDim2.new(0, 5, 0, 0), Font = Enum.Font.GothamBold }), "TextDark"),
				SetChildren(SetProps(MakeElement("TFrame"), { Position = UDim2.new(0, 0, 0, 24), AutomaticSize = Enum.AutomaticSize.Y, Name = "Holder" }), { MakeElement("List", 0, 8) })
			})
			local SecElements = {}
			-- Recursão para elementos dentro da Section
			function SecElements:AddButton(C) return Elements:AddButton(C, Section.Holder) end
			function SecElements:AddToggle(C) return Elements:AddToggle(C, Section.Holder) end
			function SecElements:AddSlider(C) return Elements:AddSlider(C, Section.Holder) end
			function SecElements:AddDropdown(C) return Elements:AddDropdown(C, Section.Holder) end
			function SecElements:AddParagraph(t,c) return Elements:AddParagraph(t,c, Section.Holder) end
			function SecElements:AddLabel(t) return Elements:AddLabel(t, Section.Holder) end
			function SecElements:AddTextbox(C) return Elements:AddTextbox(C, Section.Holder) end
			return SecElements
		end

		function Elements:AddButton(Config, Parent)
			local BtnFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 36) }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 14), { Size = UDim2.new(1, -40, 1, 0), Position = UDim2.new(0, 12, 0, 0), Font = Enum.Font.GothamBold }), "Text"),
				AddThemeObject(SetProps(MakeElement("Image", "rbxassetid://3944703587"), { Size = UDim2.new(0, 20, 0, 20), Position = UDim2.new(1, -30, 0, 8) }), "TextDark"),
				SetProps(MakeElement("Button"), { Size = UDim2.new(1, 0, 1, 0) })
			}), "Second")
			BtnFrame.TextButton.MouseButton1Click:Connect(function() 
				spawn(Config.Callback)
				TweenService:Create(BtnFrame, TweenInfo.new(0.1), {Size = UDim2.new(1, -4, 0, 32)}):Play() wait(0.1)
				TweenService:Create(BtnFrame, TweenInfo.new(0.1), {Size = UDim2.new(1, 0, 0, 36)}):Play()
			end)
		end

		function Elements:AddToggle(Config, Parent)
			local Toggled = Config.Default or false
			local TogFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 36) }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 14), { Size = UDim2.new(1, -60, 1, 0), Position = UDim2.new(0, 12, 0, 0), Font = Enum.Font.GothamBold }), "Text"),
				SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(50,50,50), 1, 0), { Size = UDim2.new(0, 42, 0, 22), Position = UDim2.new(1, -54, 0.5, 0), AnchorPoint = Vector2.new(0, 0.5), Name = "Track" }), { SetProps(MakeElement("RoundFrame", Color3.fromRGB(255,255,255), 1, 0), { Size = UDim2.new(0, 16, 0, 16), Position = UDim2.new(0, 3, 0.5, 0), AnchorPoint = Vector2.new(0, 0.5), Name = "Dot" }) }),
				SetProps(MakeElement("Button"), { Size = UDim2.new(1, 0, 1, 0) })
			}), "Second")
			
			local function Update()
				local Color = Toggled and OrionLib.Themes[OrionLib.SelectedTheme].Accent or Color3.fromRGB(50,50,50)
				local Pos = Toggled and UDim2.new(1, -19, 0.5, 0) or UDim2.new(0, 3, 0.5, 0)
				TweenService:Create(TogFrame.Track, TweenInfo.new(0.3), {BackgroundColor3 = Color}):Play()
				TweenService:Create(TogFrame.Track.Dot, TweenInfo.new(0.3), {Position = Pos}):Play()
				pcall(Config.Callback, Toggled)
			end
			Update()
			TogFrame.TextButton.MouseButton1Click:Connect(function() Toggled = not Toggled Update() end)
		end

		function Elements:AddParagraph(Title, Content, Parent)
			AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, AutomaticSize = Enum.AutomaticSize.Y }), {
				AddThemeObject(SetProps(MakeElement("Label", Title, 15), { Size = UDim2.new(1, -24, 0, 20), Position = UDim2.new(0, 12, 0, 10), Font = Enum.Font.GothamBold }), "Text"),
				AddThemeObject(SetProps(MakeElement("Label", Content, 13), { Size = UDim2.new(1, -24, 0, 0), Position = UDim2.new(0, 12, 0, 32), TextWrapped = true, AutomaticSize = Enum.AutomaticSize.Y }), "TextDark"),
				MakeElement("Padding", 12, 0, 0, 0)
			}), "Second")
		end

		function Elements:AddLabel(Text, Parent)
			AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 6), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 26) }), {
				AddThemeObject(SetProps(MakeElement("Label", Text, 14), { Size = UDim2.new(1, -20, 1, 0), Position = UDim2.new(0, 10, 0, 0) }), "Text")
			}), "Second")
		end

		function Elements:AddDropdown(Config, Parent)
			local Expanded = false
			local DropFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 36), ClipsDescendants = true }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 14), { Size = UDim2.new(1, -40, 0, 36), Position = UDim2.new(0, 12, 0, 0), Font = Enum.Font.GothamBold }), "Text"),
				AddThemeObject(SetProps(MakeElement("Image", "rbxassetid://6031090990"), { Size = UDim2.new(0, 20, 0, 20), Position = UDim2.new(1, -30, 0, 8), Name = "Ico" }), "TextDark"),
				SetProps(MakeElement("Button"), { Size = UDim2.new(1, 0, 0, 36), Name = "MainBtn" }),
				SetChildren(SetProps(MakeElement("TFrame"), { Size = UDim2.new(1, 0, 0, 0), Position = UDim2.new(0, 0, 0, 36), AutomaticSize = Enum.AutomaticSize.Y, Name = "Holder" }), { MakeElement("List", 0, 2), MakeElement("Padding", 5, 5, 5, 5) })
			}), "Second")
			
			local function RefreshOptions()
				for _, v in pairs(DropFrame.Holder:GetChildren()) do if v:IsA("Frame") then v:Destroy() end end
				for _, opt in pairs(Config.Options) do
					local OptBtn = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 6), { Parent = DropFrame.Holder, Size = UDim2.new(1, 0, 0, 26) }), {
						AddThemeObject(SetProps(MakeElement("Label", opt, 13), { Size = UDim2.new(1, -10, 1, 0), Position = UDim2.new(0, 5, 0, 0) }), "Text"),
						SetProps(MakeElement("Button"), { Size = UDim2.new(1, 0, 1, 0) })
					}), "Main")
					OptBtn.TextButton.MouseButton1Click:Connect(function()
						Config.Callback(opt)
						DropFrame.TextLabel.Text = Config.Name .. ": " .. opt
						Expanded = false
						TweenService:Create(DropFrame, TweenInfo.new(0.3), {Size = UDim2.new(1, 0, 0, 36)}):Play()
						TweenService:Create(DropFrame.Ico, TweenInfo.new(0.3), {Rotation = 0}):Play()
					end)
				end
			end
			RefreshOptions()

			DropFrame.MainBtn.MouseButton1Click:Connect(function()
				Expanded = not Expanded
				if Expanded then
					TweenService:Create(DropFrame, TweenInfo.new(0.3), {Size = UDim2.new(1, 0, 0, DropFrame.Holder.AbsoluteContentSize.Y + 42)}):Play()
					TweenService:Create(DropFrame.Ico, TweenInfo.new(0.3), {Rotation = 180}):Play()
				else
					TweenService:Create(DropFrame, TweenInfo.new(0.3), {Size = UDim2.new(1, 0, 0, 36)}):Play()
					TweenService:Create(DropFrame.Ico, TweenInfo.new(0.3), {Rotation = 0}):Play()
				end
			end)
		end

		function Elements:AddSlider(Config, Parent)
			local Min, Max, Default = Config.Min or 0, Config.Max or 100, Config.Default or 0
			local SliderFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 50) }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 14), { Size = UDim2.new(1, -30, 0, 20), Position = UDim2.new(0, 12, 0, 8), Font = Enum.Font.GothamBold }), "Text"),
				AddThemeObject(SetProps(MakeElement("Label", tostring(Default), 14), { Size = UDim2.new(0, 40, 0, 20), Position = UDim2.new(1, -52, 0, 8), TextXAlignment = Enum.TextXAlignment.Right }), "TextDark"),
				SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(50,50,50), 1, 0), { Size = UDim2.new(1, -24, 0, 6), Position = UDim2.new(0, 12, 0, 34), Name = "Track" }), {
					SetProps(MakeElement("RoundFrame", OrionLib.Themes[OrionLib.SelectedTheme].Accent, 1, 0), { Size = UDim2.new((Default - Min)/(Max - Min), 0, 1, 0), Name = "Fill" })
				}),
				SetProps(MakeElement("Button"), { Size = UDim2.new(1, 0, 1, 0), Name = "Click" })
			}), "Second")
			
			local Dragging = false
			SliderFrame.Click.MouseButton1Down:Connect(function() Dragging = true end)
			UserInputService.InputEnded:Connect(function(Input) if Input.UserInputType == Enum.UserInputType.MouseButton1 then Dragging = false end end)
			
			local function Move(Input)
				if not Dragging then return end
				local SizeX = math.clamp((Input.Position.X - SliderFrame.Track.AbsolutePosition.X) / SliderFrame.Track.AbsoluteSize.X, 0, 1)
				local Val = math.floor(Min + ((Max - Min) * SizeX))
				SliderFrame.Track.Fill.Size = UDim2.new(SizeX, 0, 1, 0)
				SliderFrame.TextLabel.Text = tostring(Val)
				pcall(Config.Callback, Val)
			end
			UserInputService.InputChanged:Connect(function(Input) if Input.UserInputType == Enum.UserInputType.MouseMovement then Move(Input) end end)
		end
		
		function Elements:AddTextbox(Config, Parent)
			local BoxFrame = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), { Parent = Parent or Container, Size = UDim2.new(1, 0, 0, 36) }), {
				AddThemeObject(SetProps(MakeElement("Label", Config.Name, 14), { Size = UDim2.new(0, 100, 1, 0), Position = UDim2.new(0, 12, 0, 0), Font = Enum.Font.GothamBold }), "Text"),
				SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(40,40,40), 0, 6), { Size = UDim2.new(1, -130, 0, 24), Position = UDim2.new(0, 120, 0, 6) }), {
					AddThemeObject(SetProps(Create("TextBox", { Size = UDim2.new(1, -10, 1, 0), Position = UDim2.new(0, 5, 0, 0), BackgroundTransparency = 1, Text = Config.Default or "", ClearTextOnFocus = false, Font = Enum.Font.Gotham }), "Text")
				})
			}), "Second")
			BoxFrame.Frame.TextBox.FocusLost:Connect(function(Enter) if Enter then Config.Callback(BoxFrame.Frame.TextBox.Text) end end)
		end

		return Elements
	end
	return Functions
end

return OrionLib
